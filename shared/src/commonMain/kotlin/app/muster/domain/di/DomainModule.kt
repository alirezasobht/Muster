package app.muster.domain.di

import app.muster.domain.usecase.AcceptGroupInvitationUseCase
import app.muster.domain.usecase.AddPlayersUseCase
import app.muster.domain.usecase.ArchiveGroupUseCase
import app.muster.domain.usecase.CreateEventUseCase
import app.muster.domain.usecase.CreateGroupUseCase
import app.muster.domain.usecase.DeclineGroupInvitationUseCase
import app.muster.domain.usecase.DeleteAccountUseCase
import app.muster.domain.usecase.DemoteMemberUseCase
import app.muster.domain.usecase.DisinvitePlayerUseCase
import app.muster.domain.usecase.GetEventUseCase
import app.muster.domain.usecase.GetGroupUseCase
import app.muster.domain.usecase.GetInviteeCandidatesUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.InviteByEmailUseCase
import app.muster.domain.usecase.LeaveGroupUseCase
import app.muster.domain.usecase.ListGroupMembersUseCase
import app.muster.domain.usecase.ListMyGroupsUseCase
import app.muster.domain.usecase.ListPendingInvitationsUseCase
import app.muster.domain.usecase.ListUpcomingEventsUseCase
import app.muster.domain.usecase.ObserveSessionUseCase
import app.muster.domain.usecase.PromoteMemberUseCase
import app.muster.domain.usecase.RemoveMemberUseCase
import app.muster.domain.usecase.ReorderStandbyUseCase
import app.muster.domain.usecase.RequestSignInCodeUseCase
import app.muster.domain.usecase.ResendEventInvitationUseCase
import app.muster.domain.usecase.ResendInvitationUseCase
import app.muster.domain.usecase.RetrySessionUseCase
import app.muster.domain.usecase.RevokeInvitationUseCase
import app.muster.domain.usecase.SetRsvpUseCase
import app.muster.domain.usecase.SignOutUseCase
import app.muster.domain.usecase.UpdateNameUseCase
import app.muster.domain.usecase.VerifySignInCodeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::RequestSignInCodeUseCase)
    factoryOf(::VerifySignInCodeUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::RetrySessionUseCase)
    factoryOf(::GetMyProfileUseCase)
    factoryOf(::UpdateNameUseCase)
    factoryOf(::ListMyGroupsUseCase)
    factoryOf(::CreateGroupUseCase)
    factoryOf(::GetGroupUseCase)
    factoryOf(::GetMyGroupRoleUseCase)
    factoryOf(::ListPendingInvitationsUseCase)
    factoryOf(::AcceptGroupInvitationUseCase)
    factoryOf(::DeclineGroupInvitationUseCase)
    factoryOf(::ListGroupMembersUseCase)
    factoryOf(::ListUpcomingEventsUseCase)
    factoryOf(::CreateEventUseCase)
    factoryOf(::GetEventUseCase)
    factoryOf(::GetInviteeCandidatesUseCase)
    factoryOf(::SetRsvpUseCase)
    factoryOf(::ReorderStandbyUseCase)
    factoryOf(::InviteByEmailUseCase)
    factoryOf(::PromoteMemberUseCase)
    factoryOf(::DemoteMemberUseCase)
    factoryOf(::RemoveMemberUseCase)
    factoryOf(::RevokeInvitationUseCase)
    factoryOf(::ResendInvitationUseCase)
    factoryOf(::LeaveGroupUseCase)
    factoryOf(::ArchiveGroupUseCase)
    factoryOf(::DisinvitePlayerUseCase)
    factoryOf(::ResendEventInvitationUseCase)
    factoryOf(::AddPlayersUseCase)
    factoryOf(::DeleteAccountUseCase)
}
