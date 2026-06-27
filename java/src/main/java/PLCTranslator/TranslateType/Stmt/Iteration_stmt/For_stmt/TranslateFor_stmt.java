package PLCTranslator.TranslateType.Stmt.Iteration_stmt.For_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.GvlContext;
import antlr4.PLCSTPARSERParser;

public class TranslateFor_stmt {
    public String translateNode(PLCSTPARSERParser.For_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        //*********************************************翻译for循环条件****************************************************
        PLCVariable controlVariable = PLCTranslatorNew.getVariable(ctx.control_variable(), "for control variable");
        String conVar = controlVariable.getName();
        PLCVariable varExpression1 = PLCTranslatorNew.getVariable(ctx.expression(0), "for start expression");
        PLCVariable varExpression2 = PLCTranslatorNew.getVariable(ctx.expression(1), "for end expression");
        String stepVar = null;
        if(ctx.by_list()!=null){
            PLCVariable varStepSize = PLCTranslatorNew.getVariable(ctx.by_list(), "for step expression");
            stepVar = varStepSize.getAssignVar();
        }

        GvlContext gvlCtx = translatorNew.gvlCtx;
        String cleanVar = conVar.startsWith("*") ? conVar.substring(1) : conVar;
        boolean isGvlVar = gvlCtx.offsetMap.containsKey(cleanVar);

        if (isGvlVar) {
            String type = gvlCtx.typeMap.getOrDefault(cleanVar, "INT");
            int offset = gvlCtx.offsetMap.get(cleanVar);
            gvlCtx.shadowedGvlVars.add(cleanVar);
            gvlCtx.shadowStack.addLast(new GvlContext.ShadowEntry(cleanVar, type, offset));
            if(gvlCtx.locallyDeclaredGvlVars.contains(cleanVar)){
                sb.append("\n\t\t").append(cleanVar).append(" = ")
                  .append(gvlCtx.translateExpr(varExpression1.getAssignVar())).append(";");
            }else{
                gvlCtx.locallyDeclaredGvlVars.add(cleanVar);
                sb.append("\n\t\t").append(type).append(" ").append(cleanVar).append(" = ")
                  .append(gvlCtx.translateExpr(varExpression1.getAssignVar())).append(";");
            }
            sb.append("\n\t\tfor( ; ").append(cleanVar).append(" <= ")
              .append(gvlCtx.translateExpr(varExpression2.getAssignVar())).append(";");
        } else {
            sb.append("\n\t\tfor( ").append(cleanVar).append(" = ")
              .append(gvlCtx.translateExpr(varExpression1.getAssignVar())).append(";");
            sb.append(cleanVar).append(" <= ").append(gvlCtx.translateExpr(varExpression2.getAssignVar())).append(";");
        }

        if (stepVar != null && !stepVar.isEmpty()) {
            sb.append(cleanVar).append(" = ").append(cleanVar).append(" + ")
              .append(gvlCtx.translateExpr(stepVar)).append("){");
        } else {
            sb.append(cleanVar).append("++){");
        }

        String bodyResult = translatorNew.visit(ctx.stmt_list());
        if (bodyResult != null) {
            sb.append(bodyResult);
        }

        sb.append("\n\t\t}");
        if (!gvlCtx.shadowStack.isEmpty()) {
            GvlContext.ShadowEntry entry = gvlCtx.shadowStack.removeLast();
            gvlCtx.shadowedGvlVars.remove(entry.varName);
            sb.append("\n\t\tgvl.write<").append(entry.type).append(">(")
              .append(entry.offset).append(", ").append(entry.varName).append(");");
        }
        return sb.toString();
    }
}
