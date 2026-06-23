package PLCTranslator.TranslateType.Stmt.Print_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

/**
 * 翻译
 */

public class TranslatePrint_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Print_stmtContext ctx, PLCTranslatorNew translatorNew) {
        PLCVariable varElement = (PLCVariable) PLCTranslatorNew.properties.get(ctx.print_stmt_element(0)).get(0);
//        System.out.print("print"+"(");
        writeTarget("print"+"(");
//        System.out.print(varElement.getAssignVar());
        writeTarget(varElement.getAssignVar());
        for(int t = 1; t < ctx.print_stmt_element().size(); t++){
            PLCVariable varTempElement = (PLCVariable) PLCTranslatorNew.properties.get(ctx.print_stmt_element(t)).get(0);
//            System.out.print("+"+varTempElement.getAssignVar());
            writeTarget("+"+varTempElement.getAssignVar());
        }

//        System.out.print(");");
        writeTarget(");");
        return null;
    }
}
