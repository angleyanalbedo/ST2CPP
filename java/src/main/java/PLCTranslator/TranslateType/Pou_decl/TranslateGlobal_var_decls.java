package PLCTranslator.TranslateType.Pou_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateGlobal_var_decls {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Global_var_declsContext ctx, PLCTranslatorNew translatorNew){
        //翻译变量段
        for (PLCSTPARSERParser.Global_var_declContext global_var_decl : ctx.global_var_decl()) {
            ArrayList<PLCSymbol> globalVarDecl = PLCTranslatorNew.properties.get(global_var_decl);
            //翻译每一条变量声明定义语句
            for (PLCSymbol symbol : globalVarDecl) {
                PLCVariable varSymbol = (PLCVariable) symbol;
//                System.out.println("auto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
//                        +"("+varSymbol.getAssignVar()+");");
                writeTarget("\nauto " + symbol.getRuntimeName() +"="+"new "+varSymbol.getRuntimeTypeName()
                        +"("+varSymbol.getAssignVar()+");");
            }
        }

        translatorNew.visitChildren(ctx);
        return null;
    }
}
