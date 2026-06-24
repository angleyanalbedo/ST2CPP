package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

/**
 * 翻译else语句
 */
public class TranslateElse_stmt {
    public String translateNode(PLCSTPARSERParser.Else_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        // 使用 CodeGenerator 生成 else（不关闭，由 TranslateIf_stmt 最终关闭）
        sb.append(PLCTranslatorNew.codeGen.emitElse());
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }
        return sb.toString();
    }
}
