package PLCTranslator.TranslateType.Stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateStmt {
    public String translateNode(PLCSTPARSERParser.StmtContext ctx, PLCTranslatorNew translatorNew){
        //访问该结点下唯一子节点
        String result = translatorNew.visit(ctx.getChild(0));
        return result != null ? result : "";
    }
}
