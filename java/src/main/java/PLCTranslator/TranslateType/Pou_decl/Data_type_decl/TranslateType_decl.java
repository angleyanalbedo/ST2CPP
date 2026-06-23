package PLCTranslator.TranslateType.Pou_decl.Data_type_decl;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateType_decl {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Type_declContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visitChildren(ctx);
        return null;
    }
}
