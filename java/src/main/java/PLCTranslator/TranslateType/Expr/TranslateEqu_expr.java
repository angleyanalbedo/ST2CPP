package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateEqu_expr {
    public String translateNode(PLCSTPARSERParser.Equ_exprContext ctx, PLCTranslatorNew t) {
        return PLCTranslatorNew.translateBinaryChain(ctx, 1, 2, t);
    }
}
