package PLCTranslator.TranslateType.Namespace_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateNamespace_decl {
    public String translateNode(PLCSTPARSERParser.Namespace_declContext ctx, PLCTranslatorNew translatorNew){
        String result = translatorNew.visit(ctx.namespace_elements());
        return result != null ? result : "";
    }
}
