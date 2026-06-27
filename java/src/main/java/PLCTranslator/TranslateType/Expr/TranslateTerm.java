package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateTerm {
    public String translateNode(PLCSTPARSERParser.TermContext ctx, PLCTranslatorNew t) {
        return PLCTranslatorNew.translateBinaryChain(ctx, 1, 2, t);
    }
}
