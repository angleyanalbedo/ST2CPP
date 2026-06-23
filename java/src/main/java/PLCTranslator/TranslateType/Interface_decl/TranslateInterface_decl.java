package PLCTranslator.TranslateType.Interface_decl;

import PLCSymbolAndScope.PLCSymbols.PLCInterfaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateInterface_decl {

    public ArrayList<String> translateNode(PLCSTPARSERParser.Interface_declContext ctx, PLCTranslatorNew translatorNew){
        for (PLCSTPARSERParser.Method_prototypeContext method_prototypeContext : ctx.method_prototype()) {
            translatorNew.visit(method_prototypeContext);
        }


        return null;
    }
}
