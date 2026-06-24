package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译else语句
 */
public class TranslateElse_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Else_stmtContext ctx, PLCTranslatorNew translatorNew){
        // 使用 CodeGenerator 生成 else（不关闭，由 TranslateIf_stmt 最终关闭）
        PLCTranslatorNew.codeGen.emitElse();
        translatorNew.visit(ctx.stmt_list());
        return null;
    }
}
