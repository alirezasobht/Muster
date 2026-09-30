package app.muster.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

class ConstructorAnnotationWrappingRule :
    Rule(RULE_ID, About()),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.PRIMARY_CONSTRUCTOR) return

        val constructor = node.psi as KtPrimaryConstructor
        if (constructor.annotationEntries.isEmpty()) return

        joinLineBreak(node.treePrev, emit)
        constructor.annotationEntries.forEach { annotation ->
            joinLineBreak(annotation.node.lastChildLeafOrSelf20.nextLeaf, emit)
        }
    }

    private fun joinLineBreak(
        node: ASTNode?,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node?.elementType != ElementType.WHITE_SPACE || !node.text.contains('\n')) return

        if (
            emit(
                node.startOffset,
                "An annotated primary constructor must stay on the class declaration line",
                true,
            ) == AutocorrectDecision.ALLOW_AUTOCORRECT
        ) {
            (node.psi as LeafPsiElement).rawReplaceWithText(" ")
        }
    }

    companion object {
        val RULE_ID = RuleId("muster:constructor-annotation-wrapping")
    }
}
