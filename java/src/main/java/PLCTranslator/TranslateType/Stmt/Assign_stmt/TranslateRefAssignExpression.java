package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateRefAssignExpression {
    public String translateNode(PLCSTPARSERParser.RefAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        String result = translatorNew.visit(ctx.getChild(0));
        return result != null ? result : "";
    }
}
