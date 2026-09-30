package app.muster.domain.usecase

import app.muster.domain.model.MemberListing
import app.muster.domain.repository.MemberRepository

class ListGroupMembersUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(groupId: String): MemberListing = members.listMembers(groupId)
}
