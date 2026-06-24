package PLCTranslator.TranslateType.Pou_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;

public class TranslateGlobal_var_decls {
    public String translateNode(PLCSTPARSERParser.Global_var_declsContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //翻译变量段
        for (PLCSTPARSERParser.Global_var_declContext global_var_decl : ctx.global_var_decl()) {
            ArrayList<PLCSymbol> globalVarDecl = PLCTranslatorNew.properties.get(global_var_decl);
            //翻译每一条变量声明定义语句
            for (PLCSymbol symbol : globalVarDecl) {
                PLCVariable varSymbol = (PLCVariable) symbol;
//                System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
//                        +"("+varSymbol.getAssignVar()+");");
                sb.append("\nauto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
                        +"("+varSymbol.getAssignVar()+");");
            }
        }

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (!(child instanceof PLCSTPARSERParser.Global_var_declContext)) {
                String result = translatorNew.visit(child);
                sb.append(result);
            }
        }
        return sb.toString();
    }
}
