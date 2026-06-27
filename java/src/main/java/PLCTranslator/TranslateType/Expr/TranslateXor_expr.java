package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateXor_expr {
    public String translateNode(PLCSTPARSERParser.Xor_exprContext ctx, PLCTranslatorNew t) {
        return PLCTranslatorNew.translateBinaryChain(ctx, 1, 2, t);
    }
}
