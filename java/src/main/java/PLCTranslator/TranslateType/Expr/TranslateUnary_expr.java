package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateUnary_expr {
    public String translateNode(PLCSTPARSERParser.Unary_exprContext ctx, PLCTranslatorNew t) {
        if (ctx.primary_expr() != null) {
            String child = (String) t.visit(ctx.primary_expr());
            if (ctx.getChildCount() == 2) {
                String op = ctx.getChild(0).getText();
                return PLCTranslatorNew.mapOperator(op) + " " + child;
            }
            return child;
        }
        return ctx.getText();
    }
}
