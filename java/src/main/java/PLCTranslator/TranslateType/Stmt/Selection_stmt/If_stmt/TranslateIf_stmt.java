package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译if选择语句
 */
public class TranslateIf_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.If_stmtContext ctx, PLCTranslatorNew translatorNew){

        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);

//        System.out.print("if("+varExpression.getAssignVar()+"){");
        writeTarget("if("+varExpression.getAssignVar()+"){");
        translatorNew.visit(ctx.stmt_list());
//        System.out.println("}");
        writeTarget("\n}");
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
