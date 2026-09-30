package app.muster.ktlint

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class ConstructorAnnotationWrappingRuleTest {
    private val ruleEngine =
        KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { ConstructorAnnotationWrappingRule() }),
        )

    @Test
    fun `moves an inject constructor onto the class declaration line`() {
        val input =
            """
            class ValidateAlertUseCase
            @Inject
            constructor() {
                fun validate() = true
            }
            """.trimIndent()
        val expected =
            """
            class ValidateAlertUseCase @Inject constructor() {
                fun validate() = true
            }
            """.trimIndent()

        assertEquals(expected, format(input))
    }

    @Test
    fun `moves an assisted inject constructor onto the class declaration line`() {
        val input =
            """
            @HiltViewModel
            class AlertFormViewModel
            @AssistedInject
            constructor(
                dependency: Dependency,
            )
            """.trimIndent()
        val expected =
            """
            @HiltViewModel
            class AlertFormViewModel @AssistedInject constructor(
                dependency: Dependency,
            )
            """.trimIndent()

        assertEquals(expected, format(input))
    }

    @Test
    fun `keeps an already compliant constructor unchanged`() {
        val code = "class Repository @Inject constructor(dependency: Dependency)"

        assertEquals(code, format(code))
    }

    @Test
    fun `does not change an annotation on the class`() {
        val code =
            """
            @Singleton
            class Repository constructor()
            """.trimIndent()

        assertEquals(code, format(code))
    }

    private fun format(code: String): String = ruleEngine.format(Code.fromSnippet(code)) { AutocorrectDecision.ALLOW_AUTOCORRECT }
}
