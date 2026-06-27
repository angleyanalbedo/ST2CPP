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
        //翻译变量段（使用 Flat 后端的 emitGlobalVarDecl 分配 GVL 偏移量，不生成文件作用域代码）
        for (PLCSTPARSERParser.Global_var_declContext global_var_decl : ctx.global_var_decl()) {
            ArrayList<PLCSymbol> globalVarDecl = PLCTranslatorNew.properties.get(global_var_decl);
            for (PLCSymbol symbol : globalVarDecl) {
                PLCVariable varSymbol = (PLCVariable) symbol;
                sb.append(translatorNew.codeGen.emitGlobalVarDecl(
                    varSymbol.getName(),
                    varSymbol.getRuntimeTypeName(),
                    varSymbol.getAssignVar(),
                    ""
                ));
            }
        }

        // 访问非变量声明子节点（如 struct 类型定义），过滤 null 防止垃圾拼接
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (!(child instanceof PLCSTPARSERParser.Global_var_declContext)) {
                String result = translatorNew.visit(child);
                if (result != null && !result.isEmpty()) {
                    sb.append(result);
                }
            }
        }
        return sb.toString();
    }
}
