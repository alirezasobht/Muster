package app.muster.domain.model

// Each key must match the flag column of a row in private.feature_flags.
enum class FeatureFlag(val key: String) {
    MemberTags("member_tags")
}
