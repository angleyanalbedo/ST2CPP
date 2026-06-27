package PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt;

import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

public class TranslateIf_stmt {
    public String translateNode(PLCSTPARSERParser.If_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();

        sb.append("\n\t\tif(").append(translatorNew.visit(ctx.expression())).append("){");
        String stmtListResult = translatorNew.visit(ctx.stmt_list());
        if (stmtListResult != null) {
            sb.append(stmtListResult);
        }

        for (PLCSTPARSERParser.Elsif_stmtContext elsif_stmtContext : ctx.elsif_stmt()) {
            String elsifResult = translatorNew.visit(elsif_stmtContext);
            if (elsifResult != null) {
                sb.append(elsifResult);
            }
        }
        if(ctx.else_stmt()!=null){
            String elseResult = translatorNew.visit(ctx.else_stmt());
            if (elseResult != null) {
                sb.append(elseResult);
            }
        }
        sb.append("\n\t\t}");
        return sb.toString();
    }
}
