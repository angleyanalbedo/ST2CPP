package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Str_type_decl;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateStr_type_decl {

    public String translateNode(PLCSTPARSERParser.Str_type_declContext ctx, PLCTranslatorNew translatorNew){
        String newName = ctx.string_type_name_identifier().getText();
        return "\nusing " + newName + " = STRING;";
    }
}
