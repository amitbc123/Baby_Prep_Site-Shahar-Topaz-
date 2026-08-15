package com.oryareach.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oryareach.core.ui.theme.OrYareachTheme

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    actions: AuthActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(
                    if (uiState.mode == AuthMode.SignIn) {
                        R.string.auth_title_sign_in
                    } else {
                        R.string.auth_title_sign_up
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = actions::onEmailChange,
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                isError = uiState.email.isNotEmpty() && !uiState.emailValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = actions::onPasswordChange,
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                isError = uiState.password.isNotEmpty() && !uiState.passwordValid,
                supportingText = {
                    Text(
                        stringResource(
                            R.string.auth_password_hint,
                            AuthUiState.MIN_PASSWORD_LENGTH,
                        ),
                    )
                },
                visualTransformation = if (uiState.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = actions::onTogglePasswordVisibility) {
                        Text(
                            stringResource(
                                if (uiState.passwordVisible) {
                                    R.string.auth_hide_password
                                } else {
                                    R.string.auth_show_password
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { actions.onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.infoMessage?.let { message ->
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // The error is a field in state, not an effect: it must survive a rotation.
            uiState.errorMessage?.let { message ->
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { },
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = actions::onSubmit,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        stringResource(
                            if (uiState.mode == AuthMode.SignIn) {
                                R.string.auth_sign_in
                            } else {
                                R.string.auth_sign_up
                            },
                        ),
                    )
                }
            }

            TextButton(
                onClick = {
                    actions.onModeChange(
                        if (uiState.mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn,
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        if (uiState.mode == AuthMode.SignIn) {
                            R.string.auth_switch_to_sign_up
                        } else {
                            R.string.auth_switch_to_sign_in
                        },
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthPreview() {
    OrYareachTheme {
        AuthScreen(uiState = AuthUiState(email = "shahar@example.com"), actions = NoopAuthActions)
    }
}

private object NoopAuthActions : AuthActions {
    override fun onEmailChange(value: String) = Unit
    override fun onPasswordChange(value: String) = Unit
    override fun onTogglePasswordVisibility() = Unit
    override fun onModeChange(mode: AuthMode) = Unit
    override fun onSubmit() = Unit
}
