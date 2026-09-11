package app.muster.domain.di

import app.muster.domain.usecase.GetMyProfileUseCase
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
}
