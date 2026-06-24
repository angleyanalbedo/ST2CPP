package PLCTranslator.TranslateType.Stmt.Iteration_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.Objects;

public class TranslateIteration_stmt {
    public String translateNode(PLCSTPARSERParser.Iteration_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        if(ctx.EXITORCONTINUE() != null){
            if(Objects.equals(ctx.EXITORCONTINUE().getText(), "CONTINUE")){
//                System.out.println("continue;");
                sb.append("\ncontinue;");
            }else {
//                System.out.println("break;");
                sb.append("\nbreak;");
            }
        }else {
            String result = translatorNew.visit(ctx.getChild(0));
            if (result != null) {
                sb.append(result);
            }
        }
        return sb.toString();
    }
}
