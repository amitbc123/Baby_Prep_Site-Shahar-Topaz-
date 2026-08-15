package com.oryareach.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.oryareach.app.di.SessionState
import com.oryareach.core.network.auth.AuthState
import com.oryareach.feature.auth.AuthEffect
import com.oryareach.feature.auth.AuthScreen
import com.oryareach.feature.auth.AuthViewModel
import com.oryareach.feature.cycle.CycleScreen
import com.oryareach.feature.cycle.CycleViewModel
import com.oryareach.feature.pairing.PairingEffect
import com.oryareach.feature.pairing.PairingScreen
import com.oryareach.feature.pairing.PairingViewModel
import com.oryareach.feature.tasks.TasksScreen
import com.oryareach.feature.tasks.TasksViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Chooses the screen from what the device actually has, rather than from navigation history.
 *
 * Signed out shows auth; signed in without a workspace key shows pairing; both present shows
 * the app. Deriving it this way means the user cannot land back on a screen that no longer
 * applies by pressing back, and a sign-out anywhere unwinds correctly on its own.
 */
@Composable
fun SaharApp(authState: AuthState) {
    val session: SessionState = koinInject()
    val workspaceId by session.workspaceIdFlow.collectAsStateWithLifecycle()

    when {
        authState == AuthState.Unknown -> Unit

        authState == AuthState.SignedOut -> AuthRoute()

        workspaceId == null || !session.isUnlocked -> PairingRoute()

        else -> HomeRoute()
    }
}

private enum class HomeTab { Tasks, Cycle }

/**
 * A plain tab switch, not `navigation-compose`: two peer screens with no back-stack semantics
 * between them don't need a `NavHost`. Real navigation arrives with folders/documents in a
 * later milestone, once there is something to navigate *into*.
 */
@Composable
private fun HomeRoute() {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Tasks) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == HomeTab.Tasks,
                    onClick = { tab = HomeTab.Tasks },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text(stringResource(com.oryareach.feature.tasks.R.string.tasks_title)) },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Cycle,
                    onClick = { tab = HomeTab.Cycle },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text(stringResource(com.oryareach.feature.cycle.R.string.cycle_title)) },
                )
            }
        },
    ) { padding ->
        when (tab) {
            HomeTab.Tasks -> TasksRoute(modifier = androidx.compose.ui.Modifier.padding(padding))
            HomeTab.Cycle -> CycleRoute(modifier = androidx.compose.ui.Modifier.padding(padding))
        }
    }
}

@Composable
private fun TasksRoute(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: TasksViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TasksScreen(uiState = uiState, actions = viewModel, modifier = modifier)
}

@Composable
private fun CycleRoute(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: CycleViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CycleScreen(uiState = uiState, actions = viewModel, modifier = modifier)
}

@Composable
private fun AuthRoute(viewModel: AuthViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    // Recomposition is driven by the auth state flow; the effect exists so a
                    // future snackbar has somewhere to hang.
                    AuthEffect.SignedIn -> Unit
                }
            }
        }
    }

    AuthScreen(uiState = uiState, actions = viewModel)
}

@Composable
private fun PairingRoute(viewModel: PairingViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    PairingEffect.Completed -> Unit
                }
            }
        }
    }

    PairingScreen(uiState = uiState, actions = viewModel)
}
