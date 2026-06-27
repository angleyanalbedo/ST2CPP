package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Derived_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateDerived_type_decl {

    public String translateNode(PLCSTPARSERParser.Derived_type_declContext ctx, PLCTranslatorNew translatorNew){
        String newName = ctx.derived_type_name().getText();
        PLCVariable typedefSymbol = PLCTranslatorNew.getVariable(ctx.derived_spec_init(), "derived type spec");
        String baseType = translatorNew.gvlCtx.toNativeType(typedefSymbol.getRuntimeTypeName());
        return "\nusing " + newName + " = " + baseType + ";";
    }
}
