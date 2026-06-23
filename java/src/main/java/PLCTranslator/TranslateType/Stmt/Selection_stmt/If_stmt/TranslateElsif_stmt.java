package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译else if语句
 */
public class TranslateElsif_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Elsif_stmtContext ctx, PLCTranslatorNew translatorNew){
        PLCVariable varExpression = (PLCVariable) PLCTranslatorNew.properties.get(ctx.expression()).get(0);
//        System.out.println("else if("+ varExpression.getAssignVar() +"){");
        writeTarget("\nelse if("+ varExpression.getAssignVar() +"){");
        translatorNew.visit(ctx.stmt_list());

//        System.out.println("}");
        writeTarget("\n}");
        return null;
    }
}
