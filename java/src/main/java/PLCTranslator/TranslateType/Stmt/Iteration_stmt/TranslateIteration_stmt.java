package PLCTranslator.TranslateType.Stmt.Iteration_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.Objects;

import static PLCTargetFileOutPut.TargetFileOutput.writeTarget;

public class TranslateIteration_stmt {
    public ArrayList<String> translateNode(PLCSTPARSERParser.Iteration_stmtContext ctx, PLCTranslatorNew translatorNew){
        if(ctx.EXITORCONTINUE() != null){
            if(Objects.equals(ctx.EXITORCONTINUE().getText(), "CONTINUE")){
//                System.out.println("continue;");
                writeTarget("\ncontinue;");
            }else {
//                System.out.println("break;");
                writeTarget("\nbreak;");
            }
        }else {
            translatorNew.visit(ctx.getChild(0));
        }
        return null;
    }
}
