package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

public class TranslateRefAssignExpression {
    public ArrayList<String> translateNode(PLCSTPARSERParser.RefAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        translatorNew.visit(ctx.getChild(0));
        return null;
    }
}
