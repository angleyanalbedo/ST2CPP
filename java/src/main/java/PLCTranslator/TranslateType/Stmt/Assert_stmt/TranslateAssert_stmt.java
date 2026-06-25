package PLCTranslator.TranslateType.Stmt.Assert_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateAssert_stmt {
    public String translateNode(PLCSTPARSERParser.Assert_stmtContext ctx, PLCTranslatorNew translatorNew) {
        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "assert expression");
        String sourceExpr = ctx.expression().getText();
        int line = ctx.getStart().getLine();
        return translatorNew.codeGen.emitAssert(varExpression.getAssignVar(), sourceExpr, line);
    }
}
