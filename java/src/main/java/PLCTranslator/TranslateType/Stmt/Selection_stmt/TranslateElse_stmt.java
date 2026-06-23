package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译else语句
 */
public class TranslateElse_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Else_stmtContext ctx, PLCTranslatorNew translatorNew){
        // 使用 CodeGenerator 生成 else
        PLCTranslatorNew.codeGen.emitElse();
        translatorNew.visit(ctx.stmt_list());
        PLCTranslatorNew.codeGen.emitIfEnd();
        return null;
    }
}
