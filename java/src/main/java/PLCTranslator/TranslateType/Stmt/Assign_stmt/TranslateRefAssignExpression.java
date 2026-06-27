package PLCTranslator.TranslateType.Stmt.Assign_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateRefAssignExpression {
    public String translateNode(PLCSTPARSERParser.RefAssignExpressionContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        String result = translatorNew.visit(ctx.getChild(0));
        for (String decl : translatorNew.pendingDecls) {
            sb.append(decl);
        }
        translatorNew.pendingDecls.clear();
        if (result != null) {
            sb.append(result);
        }
        return sb.toString();
    }
}
