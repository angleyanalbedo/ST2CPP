package PLCTranslator.TranslateType.Stmt.Print_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译print语句
 */

public class TranslatePrint_stmt {
    public String translateNode(PLCSTPARSERParser.Print_stmtContext ctx, PLCTranslatorNew translatorNew) {
        StringBuilder sb = new StringBuilder();
        PLCVariable varElement = (PLCVariable) PLCTranslatorNew.properties.get(ctx.print_stmt_element(0)).get(0);
        // 使用 CodeGenerator 生成 print
        // 拼接所有 print 元素
        StringBuilder expr = new StringBuilder(varElement.getAssignVar());
        for(int t = 1; t < ctx.print_stmt_element().size(); t++){
            PLCVariable varTempElement = (PLCVariable) PLCTranslatorNew.properties.get(ctx.print_stmt_element(t)).get(0);
            expr.append(" + ").append(varTempElement.getAssignVar());
        }
        sb.append(PLCTranslatorNew.codeGen.emitPrintStmt(expr.toString()));
        return sb.toString();
    }
}
