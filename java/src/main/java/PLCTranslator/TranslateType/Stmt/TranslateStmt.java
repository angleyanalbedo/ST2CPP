package PLCTranslator.TranslateType.Stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateStmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.StmtContext ctx, PLCTranslatorNew translatorNew){
        //访问该结点下唯一子节点
        translatorNew.visit(ctx.getChild(0));
        return null;
    }
}
