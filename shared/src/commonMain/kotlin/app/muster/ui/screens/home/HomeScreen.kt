package app.muster.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_accept
import muster.shared.generated.resources.action_decline
import muster.shared.generated.resources.home_empty_body
import muster.shared.generated.resources.home_empty_title
import muster.shared.generated.resources.home_failed_title
import muster.shared.generated.resources.home_failed_try_again
import muster.shared.generated.resources.home_invitation_label
import muster.shared.generated.resources.home_invited_by
import muster.shared.generated.resources.home_new_group
import muster.shared.generated.resources.home_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

data class HomeGroup(val id: String, val name: String)
data class HomeInvitation(val id: String, val groupName: String, val invitedByName: String? = null)

data class HomeActions(
    val onGroupClick: (String) -> Unit,
    val onNewGroupClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onAccept: (String) -> Unit,
    val onDecline: (String) -> Unit,
    val onRefresh: () -> Unit = {}
)

@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onNewGroupClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    when (val current = viewModel.state.collectAsStateWithLifecycle().value) {
        is HomeUiState.Loading -> HomeLoadingScreen(onSettingsClick = onSettingsClick)
        is HomeUiState.Error -> HomeFailedScreen(
            error = current.error,
            onSettingsClick = onSettingsClick,
            onRetry = viewModel::onRetry
        )
        is HomeUiState.Success -> HomeScreen(
            myGroups = current.groups.map { HomeGroup(it.id, it.name) },
            invitations = current.invitations.map { HomeInvitation(it.id, it.group.name, it.invitedByName) },
            canCreateGroups = current.canCreateGroups,
            signedInEmail = current.signedInEmail,
            actions = HomeActions(
                onGroupClick = onGroupClick,
                onNewGroupClick = onNewGroupClick,
                onSettingsClick = onSettingsClick,
                onAccept = viewModel::onAccept,
                onDecline = viewModel::onDecline,
                onRefresh = viewModel::onRefresh
            ),
            respondingTo = current.respondingTo,
            isRefreshing = current.isRefreshing,
            actionErrorMessage = current.actionError?.toMessage(),
            failedInvitationId = current.failedInvitationId
        )
    }
}

// App bar holds position across Loading (1s), Success (1d/1e) and Error (1t).
@Composable
private fun HomeAppBar(onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Muster",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSettingsClick) {
                Text(stringResource(Res.string.home_settings))
            }
        }
        HorizontalDivider(color = MusterColors.Hairline)
    }
}

// 1s. First load only — a refresh never comes back here (see HomeScreen).
@Composable
fun HomeLoadingScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    spinnerDelayMillis: Long = 400
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                HomeAppBar(onSettingsClick = onSettingsClick)
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MusterSpinner(delayMillis = spinnerDelayMillis)
                }
            }
        }
    }
}

// 1t. The first load itself failing — reuses the empty state's frame.
@Composable
fun HomeFailedScreen(
    error: DomainError,
    onSettingsClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                HomeAppBar(onSettingsClick = onSettingsClick)
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HomeMessageState(
                        title = stringResource(Res.string.home_failed_title),
                        body = error.toMessage()
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.5.dp, MusterColors.InText),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MusterColors.InText),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.home_failed_try_again),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    myGroups: List<HomeGroup>,
    invitations: List<HomeInvitation>,
    canCreateGroups: Boolean,
    signedInEmail: String,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    respondingTo: String? = null,
    isRefreshing: Boolean = false,
    actionErrorMessage: String? = null,
    failedInvitationId: String? = null
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                    HomeAppBar(onSettingsClick = actions.onSettingsClick)

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = actions.onRefresh,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (myGroups.isEmpty() && invitations.isEmpty()) {
                            // A plain Box emits no scroll events, so
                            // PullToRefreshBox never sees the gesture. A
                            // single full-height item keeps it centred and
                            // scrollable.
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        HomeEmptyState(signedInEmail = signedInEmail)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(invitations, key = { it.id }) { invitation ->
                                    InvitationCard(
                                        invitation = invitation,
                                        responding = respondingTo == invitation.id,
                                        errorMessage = actionErrorMessage
                                            .takeIf { failedInvitationId == invitation.id },
                                        onAccept = { actions.onAccept(invitation.id) },
                                        onDecline = { actions.onDecline(invitation.id) }
                                    )
                                }
                                items(myGroups, key = { it.id }) { group ->
                                    GroupCard(group = group, onClick = { actions.onGroupClick(group.id) })
                                }
                            }
                        }
                    }
                }

                if (canCreateGroups) {
                    val newGroupLabel = stringResource(Res.string.home_new_group)
                    ExtendedFloatingActionButton(
                        onClick = actions.onNewGroupClick,
                        icon = { Text(text = "+", style = MaterialTheme.typography.titleMedium) },
                        text = { Text(newGroupLabel) },
                        containerColor = MusterColors.Accent,
                        contentColor = MusterColors.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .semantics { contentDescription = newGroupLabel }
                    )
                }
            }
        }
    }
}

// Shared by the empty state (1e) and the failed-load state (1t) — same
// frame: dashed mark, title, one line, one trailing element.
@Composable
private fun HomeMessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DashedIconPlaceholder()
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Muted,
            textAlign = TextAlign.Center
        )
        if (trailing != null) {
            Spacer(Modifier.height(10.dp))
            trailing()
        }
    }
}

@Composable
private fun HomeEmptyState(signedInEmail: String, modifier: Modifier = Modifier) {
    HomeMessageState(
        title = stringResource(Res.string.home_empty_title),
        body = stringResource(Res.string.home_empty_body),
        modifier = modifier,
        trailing = if (signedInEmail.isNotBlank()) {
            {
                Surface(color = MusterColors.QuietSurface, shape = MaterialTheme.shapes.medium) {
                    Text(
                        text = signedInEmail,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MusterColors.Muted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }
        } else {
            null
        }
    )
}

// The dashed square above "No groups yet" / "Couldn't load your groups" —
// Compose has no built-in dashed border, so it's drawn by hand.
@Composable
private fun DashedIconPlaceholder(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val stroke = Stroke(
        width = with(density) { 1.5.dp.toPx() },
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
    )
    val radius = with(density) { 16.dp.toPx() }
    Box(
        modifier = modifier.size(56.dp).drawBehind {
            drawRoundRect(
                color = MusterColors.DashedOutline,
                cornerRadius = CornerRadius(radius, radius),
                style = stroke,
                size = Size(size.width, size.height)
            )
        }
    )
}

private val CardBorder = BorderStroke(1.dp, MusterColors.Hairline)

@Composable
private fun GroupCard(group: HomeGroup, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MusterColors.White,
        border = CardBorder,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(text = "›", style = MaterialTheme.typography.titleMedium, color = MusterColors.Muted)
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: HomeInvitation,
    responding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MusterColors.White,
        border = CardBorder,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(Res.string.home_invitation_label).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.9.sp
                ),
                color = MusterColors.Muted
            )
            Spacer(Modifier.height(4.dp))
            Text(text = invitation.groupName, style = MaterialTheme.typography.titleMedium)
            invitation.invitedByName?.let { name ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.home_invited_by, name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.Muted
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !responding,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(Res.string.action_decline), color = MusterColors.Ink)
                }
                PrimaryButton(
                    text = stringResource(Res.string.action_accept),
                    onClick = onAccept,
                    loading = responding,
                    modifier = Modifier.weight(1f)
                )
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.OutText
                )
            }
        }
    }
}

@Preview
@Composable
private fun HomeLoadingScreenPreview() {
    MusterTheme { HomeLoadingScreen(onSettingsClick = {}, spinnerDelayMillis = 0) }
}

@Preview
@Composable
private fun HomeFailedScreenPreview() {
    MusterTheme {
        HomeFailedScreen(error = DomainError.Network(), onSettingsClick = {}, onRetry = {})
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    MusterTheme {
        HomeScreen(
            myGroups = listOf(
                HomeGroup("1", "Westgate Wednesday 7s"),
                HomeGroup("2", "Sunday Social")
            ),
            invitations = listOf(HomeInvitation("3", "Thornbury Thursday", "Dan Whelan")),
            canCreateGroups = true,
            signedInEmail = "alex.doyle@gmail.com",
            actions = HomeActions(
                onGroupClick = {},
                onNewGroupClick = {},
                onSettingsClick = {},
                onAccept = {},
                onDecline = {}
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    MusterTheme {
        HomeScreen(
            myGroups = emptyList(),
            invitations = emptyList(),
            canCreateGroups = false,
            signedInEmail = "alex.doyle@gmail.com",
            actions = HomeActions(
                onGroupClick = {},
                onNewGroupClick = {},
                onSettingsClick = {},
                onAccept = {},
                onDecline = {}
            )
        )
    }
}
