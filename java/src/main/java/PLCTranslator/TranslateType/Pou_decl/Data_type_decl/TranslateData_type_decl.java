package PLCTranslator.TranslateType.Pou_decl.Data_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateData_type_decl {
    public String translateNode(PLCSTPARSERParser.Data_type_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            String result = translatorNew.visit(ctx.getChild(i));
            if (result != null) {
                sb.append(result);
            }
        }
        return sb.toString();
    }
}
