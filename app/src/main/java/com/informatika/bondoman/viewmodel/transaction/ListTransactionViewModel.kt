package com.informatika.bondoman.viewmodel.transaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatika.bondoman.model.Resource
import com.informatika.bondoman.model.local.entity.transaction.Transaction
import com.informatika.bondoman.model.repository.transaction.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListTransactionViewModel(private var transactionRepository: TransactionRepository) : ViewModel() {
    val listTransactionLiveData = MutableLiveData<Resource<List<Transaction>>>()
    private val observer = Observer<Resource<List<Transaction>>> {
        listTransactionLiveData.postValue(it)
    }

    init {
        transactionRepository.listTransactionLiveData.observeForever(observer)
    }

    fun getAllTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.getAllTransaction()
        }
    }

    override fun onCleared() {
        super.onCleared()
        transactionRepository.listTransactionLiveData.removeObserver(observer)
    }
}