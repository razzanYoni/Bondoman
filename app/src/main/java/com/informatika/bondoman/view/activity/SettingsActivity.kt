package com.informatika.bondoman.view.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.informatika.bondoman.R
import com.informatika.bondoman.databinding.ActivitySettingsBinding
import com.informatika.bondoman.model.local.ExportType
import com.informatika.bondoman.prefdatastore.jwt.JWTManager
import com.informatika.bondoman.viewmodel.login.LoginViewModel
import com.informatika.bondoman.viewmodel.transaction.ExporterTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.FileOutputStream
import java.time.LocalDateTime
import kotlin.random.Random


const val BROADCAST_TRANSACTION =
    "com.informatika.bondomanapp.receiver.RandomizeTransactionReceiver"

class SettingsActivity : NetworkAwareActivity() {
    private lateinit var mSettingsActivityBinding: ActivitySettingsBinding
    private val jwtManager: JWTManager by inject()
    private val loginViewModel: LoginViewModel by viewModel()
    private val exporterTransactionViewModel: ExporterTransactionViewModel by viewModel()
    private lateinit var exportXlsxLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportXlsLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportXlsxLauncherAndSendEmail: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSettingsActivityBinding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(mSettingsActivityBinding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val btnBroadcastTransaction = mSettingsActivityBinding.btnBroadcastTransaction
        val btnLogout = mSettingsActivityBinding.btnLogout
        val btnSend = mSettingsActivityBinding.btnSend
        val btnExportXlsx = mSettingsActivityBinding.btnExportXlsx
        val btnExportXls = mSettingsActivityBinding.btnExportXls

        btnBroadcastTransaction.setOnClickListener {
            val intent = Intent(BROADCAST_TRANSACTION)
                .putExtra("amount", Random.nextInt(0, 1000000).toDouble())
            sendBroadcast(intent)
            finish()
        }

        btnLogout.setOnClickListener {
            logout()
        }

        btnSend.setOnClickListener {
            onBtnEmailClick()
        }

        btnExportXlsx.setOnClickListener {
            exportXlsx()
        }

        btnExportXls.setOnClickListener {
            exportXls()
        }

        exportXlsxLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result.data != null) {
                        val uri = result.data!!.data
                        if (uri != null) {
                            lifecycleScope.launch {
                                exportFile(uri, ExportType.XLSX)
                            }
                        }
                    }
                }
            }

        exportXlsxLauncherAndSendEmail =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (result.data != null) {
                        val uri = result.data!!.data
                        if (uri != null) {
                            lifecycleScope.launch {
                                val success = exportFile(uri, ExportType.XLSX)
                                if (!success) {
                                    return@launch
                                }

                                sendEmail(uri)
                            }
                        }
                    }
                }
            }

        exportXlsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result.data != null) {
                        val uri = result.data!!.data
                        if (uri != null) {
                            lifecycleScope.launch {
                                exportFile(uri, ExportType.XLS)
                            }
                        }
                    }
                }
            }
    }

    private fun onBtnEmailClick() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val time = LocalDateTime.now().toString()
        val filename = "transaction_$time.xlsx"

        intent.putExtra(Intent.EXTRA_TITLE, filename)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/Download"))
        exportXlsxLauncherAndSendEmail.launch(intent)
    }

    private fun sendEmail(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "mail/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(loginViewModel.getEmail()))
                putExtra(Intent.EXTRA_SUBJECT, "Spreadsheet of Transaction")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached to this email is the spreadsheet of all of your transaction so far."
                )
                putExtra(Intent.EXTRA_STREAM, uri)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Send Transaction Through Email"))
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            jwtManager.onLogout()
            loginViewModel.logout()
            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
            Toast.makeText(this@SettingsActivity, "Logout Succesful", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finishAffinity()
        }
    }

    private suspend fun exportFile(uri: Uri, exportType: ExportType): Boolean {
        return withContext(Dispatchers.IO) {
            val mainHandler = Handler(Looper.getMainLooper())
            val descriptor = contentResolver.openFileDescriptor(uri, "w")

            val result = exporterTransactionViewModel.export(exportType)

            if (result == null) {
                mainHandler.post {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Failed to export data",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                return@withContext false
            }

            if (descriptor != null) {
                val fileOutputStream = FileOutputStream(descriptor.fileDescriptor)

                fileOutputStream.write(result.toByteArray())
                fileOutputStream.close()
                descriptor.close()

                mainHandler.post {
                    Toast.makeText(
                        this@SettingsActivity,
                        "File saved",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                return@withContext true
            } else {
                mainHandler.post {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Failed to get descriptor",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                return@withContext false
            }
        }
    }

    private fun exportXlsx() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val time = LocalDateTime.now().toString()
        val filename = "transaction_$time.xlsx"

        intent.putExtra(Intent.EXTRA_TITLE, filename)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/Download"))
        exportXlsxLauncher.launch(intent)
    }

    private fun exportXls() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/vnd.ms-excel"

        val time = LocalDateTime.now().toString()
        val filename = "transaction_$time.xls"

        intent.putExtra(Intent.EXTRA_TITLE, filename)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/Download"))
        exportXlsxLauncher.launch(intent)
    }
}