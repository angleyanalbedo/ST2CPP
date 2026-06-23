package PLCTranslator.TranslateType.Stmt.Iteration_stmt.While_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译while循环
 */

public class TranslateWhile_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.While_stmtContext ctx, PLCTranslatorNew translatorNew){
        //翻译while循环语句
        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);

        writeTarget("\nwhile("+ varExpression.getAssignVar()+
                "){");
        translatorNew.visit(ctx.stmt_list());

        writeTarget("\n}");
        return null;
    }
}
