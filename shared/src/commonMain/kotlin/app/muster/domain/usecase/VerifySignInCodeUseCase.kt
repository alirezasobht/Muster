package app.muster.domain.usecase

import app.muster.domain.repository.AuthRepository

class VerifySignInCodeUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        code: String
    ) = auth.verifySignInCode(email.trim(), code)
}
