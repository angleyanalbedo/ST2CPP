package PLCTranslator.TranslateType.Class_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateClass_decl {

    public String translateNode(PLCSTPARSERParser.Class_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        // 翻译类中方法
        for (PLCSTPARSERParser.Method_declContext method_declContext : ctx.method_decl()) {
            String result = translatorNew.visit(method_declContext);
            sb.append(result);
        }
        // 翻译类中变量声明
        PLCClassDeclSymbol classDeclSymbol = (PLCClassDeclSymbol) PLCTranslatorNew.properties.get(ctx).get(0);
        String className = classDeclSymbol.getName();

        // 类变为 C++ struct
        sb.append("\nstruct ").append(className).append(" {");
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
        sb.append("\n};");

        // 注册 CLASS 类型为 struct（与 FB 相同逻辑）
        GvlContext.StructLayout layout = new GvlContext.StructLayout(className, structFields, currentOffset);
        translatorNew.gvlCtx.registerStructType(className, layout);

        return sb.toString();
    }

}
