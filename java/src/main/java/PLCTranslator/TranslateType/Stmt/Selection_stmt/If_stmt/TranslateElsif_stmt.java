package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译else if语句
 */
public class TranslateElsif_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Elsif_stmtContext ctx, PLCTranslatorNew translatorNew){
        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);
        // 使用 CodeGenerator 生成 else if
        PLCTranslatorNew.codeGen.emitElseIf(varExpression.getAssignVar());
        translatorNew.visit(ctx.stmt_list());
        PLCTranslatorNew.codeGen.emitIfEnd();
        return null;
    }
}
