package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateXor_expr {
    public String translateNode(PLCSTPARSERParser.Xor_exprContext ctx, PLCTranslatorNew t) {
        if (ctx.and_expr().size() == 1) {
            return t.visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t.visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            sb.append(" ^ ").append(t.visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }
}
