package app.muster.domain.usecase

import app.muster.domain.repository.AuthRepository

class RequestSignInCodeUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String) = auth.requestSignInCode(email.trim())
}
