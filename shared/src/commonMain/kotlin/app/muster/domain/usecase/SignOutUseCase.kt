package app.muster.domain.usecase

import app.muster.domain.repository.AuthRepository

class SignOutUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.signOut()
}
