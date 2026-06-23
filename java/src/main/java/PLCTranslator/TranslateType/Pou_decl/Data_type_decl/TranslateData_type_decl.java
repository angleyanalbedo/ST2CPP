package PLCTranslator.TranslateType.Pou_decl.Data_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateData_type_decl {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Data_type_declContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visitChildren(ctx);
        return null;
    }
}
