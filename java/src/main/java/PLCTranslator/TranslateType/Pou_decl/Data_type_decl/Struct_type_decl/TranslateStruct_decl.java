package PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateStruct_decl {
    public String translateNode(PLCSTPARSERParser.Struct_declContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            String result = translatorNew.visit(ctx.getChild(i));
            if (result != null) {
                sb.append(result);
            }
        }
        return sb.toString();
    }
}
