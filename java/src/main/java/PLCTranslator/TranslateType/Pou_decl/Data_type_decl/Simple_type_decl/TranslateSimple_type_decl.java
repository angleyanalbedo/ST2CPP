package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Simple_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateSimple_type_decl {
    public String translateNode(PLCSTPARSERParser.Simple_type_declContext ctx, PLCTranslatorNew translatorNew){
        String newName = ctx.simple_type_name().getText();
        PLCVariable typedefSymbol = PLCTranslatorNew.getVariable(ctx.simple_spec_init(), "simple type spec");
        String baseType = translatorNew.gvlCtx.toNativeType(typedefSymbol.getRuntimeTypeName());
        return "\nusing " + newName + " = " + baseType + ";";
    }
}
