package app.muster.ui.common.util

// Cosmetic only (avatar initials) — not a permission, safe to compute here.
fun String.initials(): String {
    val parts = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
