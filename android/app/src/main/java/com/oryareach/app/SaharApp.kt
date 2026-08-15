package com.oryareach.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.oryareach.feature.update.UpdateDialog
import com.oryareach.feature.update.UpdateEffect
import com.oryareach.feature.update.UpdateViewModel
import com.oryareach.feature.shopping.ShoppingScreen
import com.oryareach.feature.shopping.ShoppingViewModel
import com.oryareach.feature.dates.DatesScreen
import com.oryareach.feature.dates.DatesViewModel
import com.oryareach.feature.home.HomeScreen
import com.oryareach.feature.home.HomeViewModel
import androidx.compose.ui.platform.LocalContext
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

    UpdateHost()
}

/**
 * Hosted at the app root, not per-screen: a mandatory update must be able to interrupt the
 * user regardless of which tab or auth state they are in.
 */
@Composable
private fun UpdateHost(viewModel: UpdateViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is UpdateEffect.OpenRelease ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))

                    is UpdateEffect.LaunchInstallConfirmation ->
                        context.startActivity(effect.intent)
                }
            }
        }
    }

    if (uiState.visible) {
        UpdateDialog(uiState = uiState, actions = viewModel)
    }
}

private enum class HomeTab { Home, Tasks, Shopping, Dates, Cycle }

/**
 * A plain tab switch, not `navigation-compose`: two peer screens with no back-stack semantics
 * between them don't need a `NavHost`. Real navigation arrives with folders/documents in a
 * later milestone, once there is something to navigate *into*.
 */
@Composable
private fun HomeRoute() {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == HomeTab.Home,
                    onClick = { tab = HomeTab.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(com.oryareach.feature.home.R.string.home_title)) },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Tasks,
                    onClick = { tab = HomeTab.Tasks },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text(stringResource(com.oryareach.feature.tasks.R.string.tasks_title)) },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Shopping,
                    onClick = { tab = HomeTab.Shopping },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text(stringResource(com.oryareach.feature.shopping.R.string.shopping_title)) },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Dates,
                    onClick = { tab = HomeTab.Dates },
                    icon = { Icon(Icons.Default.Event, contentDescription = null) },
                    label = { Text(stringResource(com.oryareach.feature.dates.R.string.dates_title)) },
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
            HomeTab.Home -> HomeTabRoute(modifier = androidx.compose.ui.Modifier.padding(padding))
            HomeTab.Tasks -> TasksRoute(modifier = androidx.compose.ui.Modifier.padding(padding))
            HomeTab.Shopping -> ShoppingRoute(modifier = androidx.compose.ui.Modifier.padding(padding))
            HomeTab.Dates -> DatesRoute(modifier = androidx.compose.ui.Modifier.padding(padding))
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
private fun HomeTabRoute(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, actions = viewModel, modifier = modifier)
}

@Composable
private fun ShoppingRoute(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: ShoppingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingScreen(uiState = uiState, actions = viewModel, modifier = modifier)
}

@Composable
private fun DatesRoute(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: DatesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DatesScreen(uiState = uiState, actions = viewModel, modifier = modifier)
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
