package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译else if语句
 */
public class TranslateElsif_stmt {
    public String translateNode(PLCSTPARSERParser.Elsif_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);
        // 使用 CodeGenerator 生成 else if（不关闭，由 TranslateIf_stmt 最终关闭）
        sb.append(PLCTranslatorNew.codeGen.emitElseIf(varExpression.getAssignVar()));
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }
        return sb.toString();
    }
}
