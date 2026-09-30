package app.muster.domain.usecase

import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveSessionUseCase(private val auth: AuthRepository) {
    operator fun invoke(): StateFlow<SessionState> = auth.session
}
