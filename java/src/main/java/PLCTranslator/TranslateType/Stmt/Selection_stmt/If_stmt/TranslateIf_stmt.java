package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

/**
 * 翻译if选择语句
 */
public class TranslateIf_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.If_stmtContext ctx, PLCTranslatorNew translatorNew){

        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);

        // 使用 CodeGenerator 生成 if
        PLCTranslatorNew.codeGen.emitIfBegin(varExpression.getAssignVar());
        translatorNew.visit(ctx.stmt_list());
        PLCTranslatorNew.codeGen.emitIfEnd();

        //翻译else if语句
        for (PLCSTPARSERParser.Elsif_stmtContext elsif_stmtContext : ctx.elsif_stmt()) {
            translatorNew.visit(elsif_stmtContext);
        }
        if(ctx.else_stmt()!=null){
            //翻译else语句
            translatorNew.visit(ctx.else_stmt());
        }
        return null;
    }
}
