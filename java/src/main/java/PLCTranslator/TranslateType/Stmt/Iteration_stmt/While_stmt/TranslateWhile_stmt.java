package PLCTranslator.TranslateType.Stmt.Iteration_stmt.While_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译while循环
 */

public class TranslateWhile_stmt {
    public String translateNode(PLCSTPARSERParser.While_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //翻译while循环语句
        PLCVariable varExpression = PLCTranslatorNew.getVariable(ctx.expression(), "while expression");

        // 使用 CodeGenerator 生成 while
        sb.append(PLCTranslatorNew.codeGen.emitWhileBegin(varExpression.getAssignVar()));
        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }
        sb.append(PLCTranslatorNew.codeGen.emitWhileEnd());
        return sb.toString();
    }
}
