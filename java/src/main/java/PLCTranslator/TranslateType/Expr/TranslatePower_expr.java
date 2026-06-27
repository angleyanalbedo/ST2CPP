package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslatePower_expr {
    public String translateNode(PLCSTPARSERParser.Power_exprContext ctx, PLCTranslatorNew t) {
        return PLCTranslatorNew.translateBinaryChain(ctx, 1, 2, t);
    }
}
