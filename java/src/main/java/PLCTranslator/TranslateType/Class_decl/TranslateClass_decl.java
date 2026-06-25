package PLCTranslator.TranslateType.Class_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

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

        // 类变为 C++ struct
        sb.append("\nstruct " + classDeclSymbol.getName() + " {");
        for (PLCVariable value : classDeclSymbol.getVariableMap().values()) {
            String nativeType = mapToNativeType(value.getRuntimeTypeName());
            sb.append("\n\t" + nativeType + " " + value.getName() + ";");
        }
        sb.append("\n};");

        return sb.toString();
    }

    /**
     * 将运行时类型名映射为原生 C++ 类型名
     */
    private String mapToNativeType(String runtimeTypeName) {
        if (runtimeTypeName == null) return "int";
        switch (runtimeTypeName) {
            case "PLC_SINT_Value": return "SINT";
            case "PLC_INT_Value": return "INT";
            case "PLC_DINT_Value": return "DINT";
            case "PLC_LINT_Value": return "LINT";
            case "PLC_Real_Value": return "REAL";
            case "PLC_LReal_Value": return "LREAL";
            case "PLC_Bool_Value": return "BOOL";
            case "PLC_String_Value": return "STRING";
            default:
                return runtimeTypeName;
        }
    }

}
