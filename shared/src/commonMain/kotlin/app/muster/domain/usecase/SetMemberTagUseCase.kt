package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class SetMemberTagUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        profileId: String,
        tagId: String?
    ) = members.setMemberTag(groupId, profileId, tagId)
}
