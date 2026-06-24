package PLCTranslator.TranslateType.Pou_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;

public class TranslatePou_decl {


    public String translateNode(PLCSTPARSERParser.Pou_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        if(ctx.using_directive()!=null){
            for (PLCSTPARSERParser.Using_directiveContext using_directiveContext : ctx.using_directive()) {
                ArrayList<PLCSymbol> usingNSList = PLCTranslatorNew.properties.get(using_directiveContext);
                for (PLCSymbol symbol : usingNSList) {
                    PLCTypeDeclSymbol tempNSSymbol = (PLCTypeDeclSymbol) symbol;
//                    System.out.println("using namespace "+tempNSSymbol.getRuntimeTypeName()+";");
                    sb.append("\nusing namespace "+tempNSSymbol.getRuntimeTypeName()+";");
                }
            }
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (!(child instanceof PLCSTPARSERParser.Using_directiveContext)) {
                String result = translatorNew.visit(child);
                sb.append(result);
            }
        }
        return sb.toString();
    }

}
