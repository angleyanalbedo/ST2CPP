package PLCTranslator.TranslateType.Pou_decl;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;

public class TranslateGlobal_var_decls {
    public String translateNode(PLCSTPARSERParser.Global_var_declsContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        for (PLCSTPARSERParser.Global_var_declContext global_var_decl : ctx.global_var_decl()) {
            ArrayList<PLCSymbol> globalVarDecl = PLCTranslatorNew.properties.get(global_var_decl);
            for (PLCSymbol symbol : globalVarDecl) {
                PLCVariable varSymbol = (PLCVariable) symbol;
                emitGlobalVarDeclInline(sb, varSymbol, translatorNew.gvlCtx);
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

    private void emitGlobalVarDeclInline(StringBuilder sb, PLCVariable varSymbol, GvlContext gvlCtx) {
        int[][] arrayBounds = varSymbol.getArrayBounds();

        if (arrayBounds != null) {
            int typeId = varSymbol.getTypeId();
            String elemTypeNative = "INT";
            if (typeId != 0) {
                PLCTypeDeclSymbol typeDecl = PLCTotalSymbolTable.getTypeByTypeID(typeId);
                if (typeDecl instanceof PLCArrayDeclSymbol) {
                    PLCArrayDeclSymbol arrayDecl = (PLCArrayDeclSymbol) typeDecl;
                    PLCTypeDeclSymbol elemType = PLCTotalSymbolTable.getTypeByTypeID(
                        arrayDecl.getElementTypeId());
                    if (elemType != null) {
                        elemTypeNative = gvlCtx.toNativeType(elemType.getName());
                    }
                }
            }
            gvlCtx.allocateArrayOffset(varSymbol.getName(), varSymbol.getArrayTotalCount(), elemTypeNative, arrayBounds);
            return;
        }

        String nativeType = gvlCtx.toNativeType(varSymbol.getRuntimeTypeName());
        gvlCtx.allocateOffset(varSymbol.getName(), nativeType);
    }
}
