package com.example.idsign

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.idsign.AppViewModelProvider.Factory
import com.example.idsign.viewModel.NetworkViewModel

class LoadingScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Loading()

            var intent: Intent = getIntent()
            val id: String? = intent.getStringExtra("ID")
            val networkViewModel = ViewModelProvider(this, Factory).get(NetworkViewModel::class.java)
            val hash by networkViewModel.hash.collectAsStateWithLifecycle()

            if (id == "Signer") {
                networkViewModel.getHash("signerapp@hcecard.com")
            } else if (id == "Signee") {
                networkViewModel.getHash("signeeapp@hcereader.com")
            }

            LaunchedEffect(hash) {
                if (hash.isNotEmpty()) {
                    networkViewModel.getPrivateKey(hash)
                }
            }

            val publicKey by networkViewModel.publicKey.collectAsStateWithLifecycle()

            if (publicKey.isNotEmpty() && hash.isNotEmpty()) {
                Log.d("hash", hash)
                Log.d("PUBLIC KEY", publicKey)

                if (id == "Signer") {
                    val intent = Intent(this, SignerActivity::class.java)
                    intent.putExtra("privateKey", publicKey)
                    intent.putExtra("hash", hash)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else if (id == "Signee") {
                    val intent = Intent(this, SigneeActivity::class.java)
                    intent.putExtra("privateKey", publicKey)
                    intent.putExtra("hash", hash)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }


        }
    }
}


@Composable
fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colors.primary,
            strokeWidth = 4.dp
        )
    }
}