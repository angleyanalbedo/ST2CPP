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
            String nativeType = translatorNew.gvlCtx.toNativeType(value.getRuntimeTypeName());
            sb.append("\n\t" + nativeType + " " + value.getName() + ";");
        }
        sb.append("\n};");

        return sb.toString();
    }

}
