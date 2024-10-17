package com.example.idsign

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.idsign.viewModel.NetworkViewModel

object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            NetworkViewModel(
                this.myIdSignApplication().container.idSignDataRespository
            )
        }
    }
}

fun CreationExtras.myIdSignApplication():  IdSignApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IdSignApplication)