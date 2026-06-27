package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateConstant_expr {
    public String translateNode(PLCSTPARSERParser.Constant_exprContext ctx, PLCTranslatorNew t) {
        return t.visit(ctx.expression());
    }
}
