package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslatePower_expr {
    public String translateNode(PLCSTPARSERParser.Power_exprContext ctx, PLCTranslatorNew t) {
        if (ctx.unary_expr().size() == 1) {
            return t.visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t.visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            sb.append(" ").append(ctx.getChild(i).getText()).append(" ").append(t.visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }
}
