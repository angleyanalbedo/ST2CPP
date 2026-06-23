package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateStruct_spec {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Struct_specContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visitChildren(ctx);
        return null;
    }
}
