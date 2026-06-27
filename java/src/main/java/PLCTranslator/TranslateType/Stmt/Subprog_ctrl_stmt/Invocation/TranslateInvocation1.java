package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateInvocation1 {
    public String translateNode(PLCSTPARSERParser.Invocation1Context ctx, PLCTranslatorNew translatorNew){
        PLCVariable instanceSymbol = PLCTranslatorNew.getVariable(ctx.invocation1branch(), "invocation1 instance");
        String className = translatorNew.gvlCtx.toNativeType(instanceSymbol.getRuntimeTypeName());
        String methodName = ctx.method_name().getText();
        boolean isThis = ctx.invocation1branch().THIS_KW() != null;

        List<String> paramValues = new ArrayList<>();
        for (PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()) {
            PLCVariable paraSymbol = PLCTranslatorNew.getVariable(param_assignContext, "invocation1 parameter");
            paramValues.add(translatorNew.gvlCtx.translateExpr(paraSymbol.getAssignVar()));
        }

        String instanceExpr = isThis ? "this" : ("&" + instanceSymbol.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t").append(className).append("_").append(methodName).append("(").append(instanceExpr);
        for (String v : paramValues) {
            sb.append(", ").append(v);
        }
        sb.append(");");
        return sb.toString();
    }
}
