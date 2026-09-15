package app.muster.domain.di

import app.muster.domain.usecase.AcceptGroupInvitationUseCase
import app.muster.domain.usecase.CreateGroupUseCase
import app.muster.domain.usecase.DeclineGroupInvitationUseCase
import app.muster.domain.usecase.GetGroupUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListMyGroupsUseCase
import app.muster.domain.usecase.ListPendingInvitationsUseCase
import app.muster.domain.usecase.ObserveSessionUseCase
import app.muster.domain.usecase.RequestSignInCodeUseCase
import app.muster.domain.usecase.RetrySessionUseCase
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
}
