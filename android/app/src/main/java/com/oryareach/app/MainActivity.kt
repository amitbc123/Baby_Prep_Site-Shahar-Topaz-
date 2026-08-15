package com.oryareach.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryareach.core.network.auth.AuthRepository
import com.oryareach.core.network.auth.AuthState
import com.oryareach.core.ui.theme.OrYareachTheme
import org.koin.compose.koinInject

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OrYareachTheme {
                val auth = koinInject<AuthRepository>()
                val authState by auth.state.collectAsStateWithLifecycle(AuthState.Unknown)
                SaharApp(authState = authState)
            }
        }
    }
}
