package PLCTranslator.TranslateType.Stmt.Assert_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateAssert_stmt {
    public String translateNode(PLCSTPARSERParser.Assert_stmtContext ctx, PLCTranslatorNew translatorNew) {
        String sourceExpr = ctx.expression().getText();
        int line = ctx.getStart().getLine();
        String cond = PLCTranslatorNew.translateExpression(ctx.expression(), translatorNew);
        String exprEscaped = sourceExpr.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\n\t\t{ bool _st_assert = (" + cond + ");" +
               "\n\t\t  if(_st_assert)" +
               "\n\t\t    printf(\"  [PASS] assert (line " + line + ")\\n\");" +
               "\n\t\t  else" +
               "\n\t\t    printf(\"  [FAIL] assert (line " + line + "): " + exprEscaped + "\\n\"); }";
    }
}
