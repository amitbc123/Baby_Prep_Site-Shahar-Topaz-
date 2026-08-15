package com.oryareach.feature.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oryareach.core.security.InvitationToken
import com.oryareach.core.ui.text.asLtrIsolate
import com.oryareach.core.ui.theme.OrYareachTheme

@Composable
fun PairingScreen(
    uiState: PairingUiState,
    actions: PairingActions,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val stage = uiState.stage) {
                PairingStage.Loading -> LoadingStage()
                PairingStage.Choose -> ChooseStage(uiState, actions)
                is PairingStage.ShowRecoveryPhrase -> RecoveryPhraseStage(stage, uiState, actions)
                PairingStage.EnterCode -> EnterCodeStage(uiState, actions)
                PairingStage.AwaitingKey -> AwaitingKeyStage(uiState, actions)
                is PairingStage.Ready -> ReadyStage(stage, uiState, actions)
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun LoadingStage() {
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ChooseStage(uiState: PairingUiState, actions: PairingActions) {
    Heading(R.string.pairing_choose_title, R.string.pairing_choose_body)

    Button(
        onClick = actions::onCreateWorkspace,
        enabled = !uiState.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.pairing_create)) }

    OutlinedButton(
        onClick = actions::onChooseJoin,
        enabled = !uiState.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.pairing_join)) }
}

@Composable
private fun RecoveryPhraseStage(
    stage: PairingStage.ShowRecoveryPhrase,
    uiState: PairingUiState,
    actions: PairingActions,
) {
    Heading(R.string.pairing_phrase_title, R.string.pairing_phrase_body)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Numbered and isolated: the words are Latin inside an otherwise RTL layout, and
            // order is the whole point of a recovery phrase.
            stage.words.forEachIndexed { index, word ->
                Text(
                    text = "${index + 1}. $word".asLtrIsolate(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = uiState.phraseConfirmed, onCheckedChange = actions::onPhraseConfirmedChange)
        Text(
            text = stringResource(R.string.pairing_phrase_confirm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    Button(
        onClick = actions::onFinishRecoveryPhrase,
        // Gated on the checkbox: this screen is the only time the phrase is ever shown.
        enabled = uiState.phraseConfirmed && !uiState.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.pairing_phrase_continue)) }
}

@Composable
private fun EnterCodeStage(uiState: PairingUiState, actions: PairingActions) {
    Heading(R.string.pairing_code_title, R.string.pairing_code_body)

    OutlinedTextField(
        value = InvitationToken.forDisplay(uiState.enteredCode),
        onValueChange = actions::onCodeChange,
        label = { Text(stringResource(R.string.pairing_code_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Button(
        onClick = actions::onSubmitCode,
        enabled = uiState.canSubmitCode,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.pairing_code_submit)) }
}

@Composable
private fun AwaitingKeyStage(uiState: PairingUiState, actions: PairingActions) {
    Heading(R.string.pairing_waiting_title, R.string.pairing_waiting_body)

    Button(
        onClick = actions::onRefresh,
        enabled = !uiState.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.pairing_check_again)) }
}

@Composable
private fun ReadyStage(
    stage: PairingStage.Ready,
    uiState: PairingUiState,
    actions: PairingActions,
) {
    Text(
        text = stringResource(R.string.pairing_ready_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )

    stage.pendingDevices.forEach { device ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.pairing_pending_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = device.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.pairing_pending_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { actions.onApproveDevice(device.deviceKeyId) },
                    enabled = !uiState.busy,
                ) { Text(stringResource(R.string.pairing_approve)) }
            }
        }
    }

    if (stage.inviteCode == null) {
        OutlinedButton(
            onClick = actions::onGenerateInvite,
            enabled = !uiState.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.pairing_invite_generate)) }
    } else {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = InvitationToken.forDisplay(stage.inviteCode).asLtrIsolate(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.pairing_invite_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Button(onClick = actions::onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.pairing_continue))
    }
}

@Composable
private fun Heading(titleRes: Int, bodyRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun PairingChoosePreview() {
    OrYareachTheme {
        PairingScreen(
            uiState = PairingUiState(stage = PairingStage.Choose),
            actions = NoopPairingActions,
        )
    }
}

private object NoopPairingActions : PairingActions {
    override fun onCreateWorkspace() = Unit
    override fun onChooseJoin() = Unit
    override fun onCodeChange(value: String) = Unit
    override fun onSubmitCode() = Unit
    override fun onPhraseConfirmedChange(confirmed: Boolean) = Unit
    override fun onFinishRecoveryPhrase() = Unit
    override fun onGenerateInvite() = Unit
    override fun onApproveDevice(deviceKeyId: String) = Unit
    override fun onRefresh() = Unit
}
