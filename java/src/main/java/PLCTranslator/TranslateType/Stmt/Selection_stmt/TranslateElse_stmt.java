package PLCTranslator.TranslateType.Stmt.Selection_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译else语句
 */
public class TranslateElse_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Else_stmtContext ctx, PLCTranslatorNew translatorNew){
//        System.out.println("else{");
        writeTarget("else{");
        translatorNew.visit(ctx.stmt_list());
//        System.out.println("}");
        writeTarget("\n}");
        return null;
    }
}
