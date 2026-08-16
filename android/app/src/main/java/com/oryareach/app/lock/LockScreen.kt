package com.oryareach.app.lock

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.oryareach.app.R
import com.oryareach.app.di.SessionState
import com.oryareach.core.security.DeviceIdentity
import org.koin.compose.koinInject

/**
 * Shown whenever [SessionState.isLocked] — a deliberate lock (auto-lock timeout, or "Lock now"
 * in Settings), distinct from never having unlocked at all. Re-arms the already-open session
 * from the device's own Keystore-sealed key copy once biometrics/device-credential succeed, so
 * unlocking never re-runs pairing and never touches the network.
 */
@Composable
fun LockRoute() {
    val session: SessionState = koinInject()
    val identity: DeviceIdentity = koinInject()
    val activity = LocalActivity.current as? FragmentActivity

    val prompt: () -> Unit = remember(activity) {
        {
            val key = identity.workspaceKey()
            if (activity != null && key != null) {
                val executor = ContextCompat.getMainExecutor(activity)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        session.unlock(key)
                    }
                }
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.lock_prompt_title))
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                    .build()
                BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
            }
        }
    }

    LaunchedEffect(activity) { prompt() }

    LockScreen(onUnlockClick = prompt)
}

@Composable
private fun LockScreen(onUnlockClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(onClick = onUnlockClick, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.lock_unlock))
            }
        }
    }
}
