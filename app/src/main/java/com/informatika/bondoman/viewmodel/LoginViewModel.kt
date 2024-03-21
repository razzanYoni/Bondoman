package com.informatika.bondoman.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.informatika.bondoman.repository.LoginRepository
import com.informatika.bondoman.network.ApiResponse

import com.informatika.bondoman.R
import com.informatika.bondoman.view.activity.login.LoginFormState
import com.informatika.bondoman.view.activity.login.LoginResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult


    fun login(username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {

            when (val result = loginRepository.login(username, password)) {
                is ApiResponse.Success -> {
                    _loginResult.postValue(LoginResult(jwtToken = result.data))
                }
                is ApiResponse.Error -> {
                    _loginResult.postValue(LoginResult(error = R.string.login_failed))
                }
            }
        }

    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_email)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    return LoginViewModel(
                        loginRepository = LoginRepository()
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}