package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Subrange_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateSubrange_type_decl {

    public String translateNode(PLCSTPARSERParser.Subrange_type_declContext ctx, PLCTranslatorNew translatorNew){
        String newName = ctx.subrange_type_name().getText();
        PLCVariable typedefSymbol = PLCTranslatorNew.getVariable(ctx.subrange_spec_init(), "subrange type spec");
        String baseType = translatorNew.gvlCtx.toNativeType(typedefSymbol.getRuntimeTypeName());
        return "\nusing " + newName + " = " + baseType + ";";
    }
}
