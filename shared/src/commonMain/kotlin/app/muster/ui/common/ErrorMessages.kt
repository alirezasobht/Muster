package app.muster.ui.common

import androidx.compose.runtime.Composable
import app.muster.domain.error.DomainError
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.error_already_invited
import muster.shared.generated.resources.error_already_member
import muster.shared.generated.resources.error_event_frozen
import muster.shared.generated.resources.error_event_full
import muster.shared.generated.resources.error_event_starts_in_past
import muster.shared.generated.resources.error_invalid_code
import muster.shared.generated.resources.error_invalid_email
import muster.shared.generated.resources.error_invalid_name
import muster.shared.generated.resources.error_invitation_not_pending
import muster.shared.generated.resources.error_invitation_sent_too_recently
import muster.shared.generated.resources.error_last_admin
import muster.shared.generated.resources.error_network
import muster.shared.generated.resources.error_not_allowed_to_create_groups
import muster.shared.generated.resources.error_not_signed_in
import muster.shared.generated.resources.error_rate_limited
import muster.shared.generated.resources.error_standby_queue_stale
import muster.shared.generated.resources.error_unknown
import org.jetbrains.compose.resources.stringResource

// The only place a DomainError becomes display text. ViewModels keep the
// error itself on their state, so no layer below the UI holds copy.
@Composable
fun DomainError.toMessage(): String = stringResource(
    when (this) {
        is DomainError.InvalidEmail -> Res.string.error_invalid_email
        is DomainError.InvalidCode -> Res.string.error_invalid_code
        is DomainError.RateLimited -> Res.string.error_rate_limited
        is DomainError.InvalidName -> Res.string.error_invalid_name
        is DomainError.InvitationNotPending -> Res.string.error_invitation_not_pending
        is DomainError.NotAllowedToCreateGroups -> Res.string.error_not_allowed_to_create_groups
        is DomainError.LastAdmin -> Res.string.error_last_admin
        is DomainError.AlreadyInvited -> Res.string.error_already_invited
        is DomainError.AlreadyMember -> Res.string.error_already_member
        is DomainError.EventStartsInPast -> Res.string.error_event_starts_in_past
        is DomainError.EventFull -> Res.string.error_event_full
        is DomainError.EventFrozen -> Res.string.error_event_frozen
        is DomainError.StandbyQueueStale -> Res.string.error_standby_queue_stale
        is DomainError.InvitationSentTooRecently -> Res.string.error_invitation_sent_too_recently
        is DomainError.Network -> Res.string.error_network
        is DomainError.NotSignedIn -> Res.string.error_not_signed_in
        is DomainError.Unknown -> Res.string.error_unknown
    }
)
