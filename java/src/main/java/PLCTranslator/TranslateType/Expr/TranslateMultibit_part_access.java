package PLCTranslator.TranslateType.Expr;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateMultibit_part_access {
    public String translateNode(PLCSTPARSERParser.Multibit_part_accessContext ctx, PLCTranslatorNew t) {
        return ctx.getText();
    }
}
