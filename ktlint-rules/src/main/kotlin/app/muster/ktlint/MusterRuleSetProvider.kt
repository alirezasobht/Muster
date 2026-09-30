package app.muster.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class MusterRuleSetProvider : RuleSetProviderV3(RuleSetId("muster")) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { AssignmentExpressionWrappingRule() },
        RuleProvider { ConstructorAnnotationWrappingRule() },
    )
}
