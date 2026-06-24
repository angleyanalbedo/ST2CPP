package PLCTranslator.TranslateType.Fb_body;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

//public class Translatefb_Body extends PLCSTPARSERBaseVisitor<ArrayList<String>>{
//    @Override public ArrayList<String> visitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx) {
//        return visitChildren(ctx);
//    }
//
//
//
//}
public class TranslateFb_body {
    public String translateNode(PLCSTPARSERParser.Fb_bodyContext ctx, PLCTranslatorNew translatorNew){
        String result = translatorNew.visit(ctx.getChild(0));
        return result != null ? result : "";
    }

}
