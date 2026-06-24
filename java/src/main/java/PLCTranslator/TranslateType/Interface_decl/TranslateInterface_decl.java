package PLCTranslator.TranslateType.Interface_decl;

import PLCSymbolAndScope.PLCSymbols.PLCInterfaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateInterface_decl {

    public String translateNode(PLCSTPARSERParser.Interface_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        for (PLCSTPARSERParser.Method_prototypeContext method_prototypeContext : ctx.method_prototype()) {
            String result = translatorNew.visit(method_prototypeContext);
            sb.append(result);
        }


        return sb.toString();
    }
}
