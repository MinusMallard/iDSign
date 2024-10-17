package com.example.idsign.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.idsign.data.IdSignDataRespository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkViewModel(
    private val myNetworkRepository: IdSignDataRespository
): ViewModel() {

    private var _hash = MutableStateFlow<String>("")
    var hash: StateFlow<String> = _hash.asStateFlow()

    private var _publicKey = MutableStateFlow<String>("")
    var publicKey: StateFlow<String> = _publicKey.asStateFlow()

    fun getHash(id: String){
        viewModelScope.launch {
            _hash.value = myNetworkRepository.getHash(id)
        }
    }

    fun getPrivateKey(id: String) {
        viewModelScope.launch {
            _publicKey.value = myNetworkRepository.getPrivateHash(id)
        }
    }
}