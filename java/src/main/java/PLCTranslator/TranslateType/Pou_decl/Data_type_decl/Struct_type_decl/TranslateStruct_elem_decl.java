package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译struct
 */
public class TranslateStruct_elem_decl {
    public String translateNode(PLCSTPARSERParser.Struct_elem_declContext ctx, PLCTranslatorNew translatorNew){
        String varName = ctx.struct_elem_name().getText();

        PLCVariable typeSymbol = (PLCVariable) PLCTranslatorNew.properties.get(ctx.getChild(1)).get(0);

        return "";
    }
}
