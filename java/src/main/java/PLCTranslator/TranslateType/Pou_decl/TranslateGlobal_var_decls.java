package PLCTranslator.TranslateType.Pou_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
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
                emitGlobalVarDeclInline(sb, varSymbol.getName(),
                    varSymbol.getRuntimeTypeName(), varSymbol.getAssignVar(), translatorNew.gvlCtx);
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

    private void emitGlobalVarDeclInline(StringBuilder sb, String name, String typeName, String assignVar, GvlContext gvlCtx) {
        if (typeName != null && typeName.startsWith("ARRAY")) {
            int count = 0;
            String elemType = "INT";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "ARRAY\\[(\\d+)\\.\\.(\\d+)\\]\\s+OF\\s+(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(typeName);
            if (matcher.find()) {
                int low = Integer.parseInt(matcher.group(1));
                int high = Integer.parseInt(matcher.group(2));
                count = high - low + 1;
                elemType = gvlCtx.toNativeType(matcher.group(3));
            }
            if (count > 0) {
                int elemSize = gvlCtx.getTypeSize(elemType);
                int totalSize = elemSize * count;
                int aligned = (gvlCtx.currentOffset + elemSize - 1) / elemSize * elemSize;
                gvlCtx.offsetMap.put(name, aligned);
                gvlCtx.typeMap.put(name, "ARRAY[" + count + "] OF " + elemType);
                gvlCtx.currentOffset = aligned + totalSize;
                return;
            }
        }
        String nativeType = gvlCtx.toNativeType(typeName);
        gvlCtx.allocateOffset(name, nativeType);
    }
}
