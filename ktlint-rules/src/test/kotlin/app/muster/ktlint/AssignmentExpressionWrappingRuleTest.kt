package app.muster.ktlint

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class AssignmentExpressionWrappingRuleTest {
    private val ruleEngine =
        KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { AssignmentExpressionWrappingRule() }),
        )

    @Test
    fun `moves a multiline constructor call onto the assignment line`() {
        assertFormatting(
            before =
                """
                val actions =
                    AlertEditActions(
                        onConfirm = viewModel::onConfirm,
                        onCancel = onDismiss,
                    )
                """,
            after =
                """
                val actions = AlertEditActions(
                        onConfirm = viewModel::onConfirm,
                        onCancel = onDismiss,
                    )
                """,
        )
    }

    @Test
    fun `moves a single-line constructor call onto the assignment line`() {
        assertFormatting(
            before =
                """
                val actions =
                    AlertEditActions(onConfirm = viewModel::onConfirm)
                """,
            after =
                """
                val actions = AlertEditActions(onConfirm = viewModel::onConfirm)
                """,
        )
    }

    @Test
    fun `moves a qualified function call onto the assignment line`() {
        assertFormatting(
            before =
                """
                val result =
                    factory.create(
                        first,
                        second,
                    )
                """,
            after =
                """
                val result = factory.create(
                        first,
                        second,
                    )
                """,
        )
    }

    @Test
    fun `moves a when expression onto a property assignment line`() {
        assertFormatting(
            before =
                """
                val priceValid =
                    when (direction) {
                        ABOVE -> targetValue > price
                        BELOW -> targetValue < price
                    }
                """,
            after =
                """
                val priceValid = when (direction) {
                        ABOVE -> targetValue > price
                        BELOW -> targetValue < price
                    }
                """,
        )
    }

    @Test
    fun `moves a multiline boolean expression onto the assignment line`() {
        assertFormatting(
            before =
                """
                val changed =
                    type != original.type ||
                        direction != original.direction
                """,
            after =
                """
                val changed = type != original.type ||
                        direction != original.direction
                """,
        )
    }

    @Test
    fun `moves a when expression onto a named argument assignment line`() {
        assertFormatting(
            before =
                """
                fun render() {
                    Text(
                        text =
                            when (type) {
                                PERCENT -> percentHint
                                else -> priceHint
                            },
                    )
                }
                """,
            after =
                """
                fun render() {
                    Text(
                        text = when (type) {
                                PERCENT -> percentHint
                                else -> priceHint
                            },
                    )
                }
                """,
        )
    }

    @Test
    fun `moves an expression onto a later assignment line`() {
        assertFormatting(
            before =
                """
                fun update() {
                    var state = Idle
                    state =
                        when (result) {
                            SUCCESS -> Ready
                            else -> Failed
                        }
                }
                """,
            after =
                """
                fun update() {
                    var state = Idle
                    state = when (result) {
                            SUCCESS -> Ready
                            else -> Failed
                        }
                }
                """,
        )
    }

    @Test
    fun `moves an if expression onto a default value assignment line`() {
        assertFormatting(
            before =
                """
                fun label(
                    value: String =
                        if (enabled) activeLabel else inactiveLabel,
                ) = value
                """,
            after =
                """
                fun label(
                    value: String = if (enabled) activeLabel else inactiveLabel,
                ) = value
                """,
        )
    }

    @Test
    fun `moves an object expression onto the assignment line`() {
        assertFormatting(
            before =
                """
                val provider =
                    object : Provider {
                        override fun get() = value
                    }
                """,
            after =
                """
                val provider = object : Provider {
                        override fun get() = value
                    }
                """,
        )
    }

    @Test
    fun `keeps an already compliant expression unchanged`() {
        val code =
            """
            val actions = AlertEditActions(
                onConfirm = viewModel::onConfirm,
            )
            """.trimIndent()

        assertEquals(code, format(code))
    }

    @Test
    fun `keeps a comment-separated expression unchanged`() {
        val code =
            """
            val actions =
                // Keep this explanation with the initializer.
                AlertEditActions()
            val other =
                /* Keep this block comment with the initializer. */
                AlertEditActions()
            """.trimIndent()

        assertEquals(code, format(code))
    }

    @Test
    fun `does not change non-assignment operators`() {
        val code =
            """
            fun update() {
                val matches = first ==
                    second
                total +=
                    amount
                val mapped = source.map { value ->
                    value
                }
            }

            fun refresh() {
                val matches = first == second
                total += amount
                val mapped = source.map { value -> value }
            }
            """.trimIndent()

        assertEquals(code, format(code))
    }

    private fun assertFormatting(
        before: String,
        after: String,
    ) {
        assertEquals(after.trimIndent(), format(before.trimIndent()))
    }

    private fun format(code: String): String = ruleEngine.format(Code.fromSnippet(code)) { AutocorrectDecision.ALLOW_AUTOCORRECT }
}
