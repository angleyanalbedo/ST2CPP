package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateExpression {
    public String translateNode(PLCSTPARSERParser.ExpressionContext ctx, PLCTranslatorNew t) {
        String firstChild = t.visit(ctx.getChild(0));
        boolean hasCompare = firstChild.contains("<") || firstChild.contains(">")
                || firstChild.contains("==") || firstChild.contains("!=");
        return PLCTranslatorNew.translateBinaryChain(ctx, 1, 2, t, hasCompare);
    }
}
