package PLCTranslator.TranslateType.Fb_body;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

//public class Translatefb_Body extends PLCSTPARSERBaseVisitor<ArrayList<String>>{
//    @Override public ArrayList<String> visitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx) {
//        return visitChildren(ctx);
//    }
//
//
//
//}
public class TranslateFb_body {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Fb_bodyContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visit(ctx.getChild(0));
        return null;
    }

}