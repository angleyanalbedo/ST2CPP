package PLCTranslator.TranslateType.Stmt.Iteration_stmt.Repeat_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译repeat循环语句
 */
public class TranslateRepeat_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Repeat_stmtContext ctx, PLCTranslatorNew translatorNew){
       //翻译do while语句
       PLCTranslatorNew.codeGen.emitRepeatBegin();
       translatorNew.visit(ctx.stmt_list());

       PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);
       //while条件语句
       PLCTranslatorNew.codeGen.emitRepeatEnd(varExpression.getAssignVar());
       return null;
    }
}
