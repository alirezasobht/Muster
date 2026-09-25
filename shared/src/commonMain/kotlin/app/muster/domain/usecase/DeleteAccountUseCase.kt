package app.muster.domain.usecase

import app.muster.domain.repository.AuthRepository

class DeleteAccountUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(force: Boolean) = auth.deleteAccount(force)
}
