package PLCTranslator.TranslateType.Pou_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslatePou_decl {


    public ArrayList<String> translateNode(PLCSTPARSERParser.Pou_declContext ctx, PLCTranslatorNew translatorNew){
        if(ctx.using_directive()!=null){
            for (PLCSTPARSERParser.Using_directiveContext using_directiveContext : ctx.using_directive()) {
                ArrayList<PLCSymbol> usingNSList = PLCTranslatorNew.properties.get(using_directiveContext);
                for (PLCSymbol symbol : usingNSList) {
                    PLCTypeDeclSymbol tempNSSymbol = (PLCTypeDeclSymbol) symbol;
//                    System.out.println("using namespace "+tempNSSymbol.getRuntimeTypeName()+";");
                    writeTarget("\nusing namespace "+tempNSSymbol.getRuntimeTypeName()+";");
                }
            }
        }
        translatorNew.visitChildren(ctx);
        return null;
    }

}
