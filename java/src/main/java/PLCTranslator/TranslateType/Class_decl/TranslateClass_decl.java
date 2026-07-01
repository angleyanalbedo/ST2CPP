package PLCTranslator.TranslateType.Class_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateClass_decl {

    public String translateNode(PLCSTPARSERParser.Class_declContext ctx, PLCTranslatorNew translatorNew){
        PLCClassDeclSymbol classDeclSymbol = (PLCClassDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String className = classDeclSymbol.getName();

        // 标记当前正在翻译 CLASS（供 TranslateMethod_decl 区分）
        translatorNew.inClassDecl = true;

        StringBuilder sb = new StringBuilder();
        sb.append("\nclass ").append(className).append(" {");

        // 成员变量（public）
        sb.append("\npublic:");
        List<GvlContext.StructField> structFields = new ArrayList<>();
        int currentOffset = 0;
        for (PLCVariable value : classDeclSymbol.getVariableMap().values()) {
            String nativeType = translatorNew.gvlCtx.toNativeType(value.getRuntimeTypeName());
            sb.append("\n\t").append(nativeType).append(" ").append(value.getName()).append(";");
            int elemSize = translatorNew.gvlCtx.getTypeSize(nativeType);
            int aligned = (currentOffset + elemSize - 1) / elemSize * elemSize;
            structFields.add(new GvlContext.StructField(value.getName(), nativeType, aligned));
            currentOffset = aligned + elemSize;
        }

        // 成员方法（在 class 内部生成）
        for (PLCSTPARSERParser.Method_declContext method_declContext : ctx.method_decl()) {
            String result = translatorNew.visit(method_declContext);
            sb.append(result);
        }

        sb.append("\n};");

        translatorNew.inClassDecl = false;

        // 注册 CLASS 类型为 struct（GVL 偏移计算用）
        GvlContext.StructLayout layout = new GvlContext.StructLayout(className, structFields, currentOffset);
        translatorNew.gvlCtx.registerStructType(className, layout);

        return sb.toString();
    }

}
