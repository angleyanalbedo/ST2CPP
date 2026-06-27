package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslatePrimary_expr {
    public String translateNode(PLCSTPARSERParser.Primary_exprContext ctx, PLCTranslatorNew t) {
        if (ctx.expression() != null) {
            return "(" + t.visit(ctx.expression()) + ")";
        }
        return (String) t.visit(ctx.getChild(0));
    }
}
