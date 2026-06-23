package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;


/**
 * 翻译引用；类型赋值
 */

public class TranslateRef_assign {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Ref_assignContext ctx, PLCTranslatorNew translatorNew){
//        System.out.println("*"+ctx.getChild(0).getText()+"="+"*"+PLCTranslatorNew.properties.get(ctx.getChild(3)));
        return null;
    }
}
