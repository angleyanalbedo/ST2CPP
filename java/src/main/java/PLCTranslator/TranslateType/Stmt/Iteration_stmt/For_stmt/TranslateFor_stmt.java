package PLCTranslator.TranslateType.Stmt.Iteration_stmt.For_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import PLCTranslator.GvlContext;
import antlr4.PLCSTPARSERParser;

public class TranslateFor_stmt {
    public String translateNode(PLCSTPARSERParser.For_stmtContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCVariable controlVariable = PLCTranslatorNew.getVariable(ctx.control_variable(), "for control variable");
        String conVar = controlVariable.getName();

        GvlContext gvlCtx = translatorNew.gvlCtx;
        String cleanVar = conVar.startsWith("*") ? conVar.substring(1) : conVar;
        boolean isGvlVar = gvlCtx.offsetMap.containsKey(cleanVar);

        // 通过 visitor 翻译 start/end/step 表达式
        String startVal = translatorNew.visit(ctx.expression(0));
        String endVal = translatorNew.visit(ctx.expression(1));
        String stepVal = (ctx.by_list() != null) ? translatorNew.visit(ctx.by_list()) : null;

        if (isGvlVar) {
            String type = gvlCtx.typeMap.getOrDefault(cleanVar, "INT");
            int offset = gvlCtx.offsetMap.get(cleanVar);
            gvlCtx.shadowedGvlVars.add(cleanVar);
            gvlCtx.shadowStack.addLast(new GvlContext.ShadowEntry(cleanVar, type, offset));
            if(gvlCtx.locallyDeclaredGvlVars.contains(cleanVar)){
                sb.append("\n\t\t").append(cleanVar).append(" = ").append(startVal).append(";");
            }else{
                gvlCtx.locallyDeclaredGvlVars.add(cleanVar);
                sb.append("\n\t\t").append(type).append(" ").append(cleanVar).append(" = ").append(startVal).append(";");
            }
            sb.append("\n\t\tfor( ; ").append(cleanVar).append(" <= ").append(endVal).append(";");
        } else {
            sb.append("\n\t\tfor( ").append(cleanVar).append(" = ").append(startVal).append(";");
            sb.append(cleanVar).append(" <= ").append(endVal).append(";");
        }

        if (stepVal != null && !stepVal.isEmpty()) {
            sb.append(cleanVar).append(" = ").append(cleanVar).append(" + ").append(stepVal).append("){");
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
