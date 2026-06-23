package PLCTranslator.TranslateType.Namespace_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateNamespace_decl {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Namespace_declContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visit(ctx.namespace_elements());
        return null;
    }
}
