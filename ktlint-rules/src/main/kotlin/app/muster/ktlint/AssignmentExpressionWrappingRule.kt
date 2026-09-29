package app.muster.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class AssignmentExpressionWrappingRule :
    Rule(RULE_ID, About()),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.WHITE_SPACE || !node.text.contains('\n')) return
        if (node.prevLeaf?.elementType != ElementType.EQ) return
        if (node.nextLeaf?.elementType in COMMENT_TYPES) return

        if (
            emit(
                node.startOffset,
                "An expression must start on the same line as its assignment",
                true,
            ) == AutocorrectDecision.ALLOW_AUTOCORRECT
        ) {
            (node.psi as LeafPsiElement).rawReplaceWithText(" ")
        }
    }

    companion object {
        val RULE_ID = RuleId("muster:assignment-expression-wrapping")

        private val COMMENT_TYPES = setOf(
            ElementType.BLOCK_COMMENT,
            ElementType.EOL_COMMENT,
            ElementType.KDOC,
        )
    }
}
