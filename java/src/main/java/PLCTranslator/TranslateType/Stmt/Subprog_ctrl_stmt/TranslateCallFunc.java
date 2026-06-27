package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt;

import PLCSymbolAndScope.PLCSymbols.PLCFBCallSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;


import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateCallFunc {
    public String translateNode(PLCSTPARSERParser.CallFuncContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        if(ctx.getChild(0) instanceof PLCSTPARSERParser.Func_callContext childCtx){
            PLCSymbol firstSym = PLCTranslatorNew.getSymbol(childCtx, "call function");
            if(firstSym instanceof PLCFBCallSymbol fbCallSym){
                String fbInstanceName = fbCallSym.getFbInstanceName();
                String fbTypeName = translatorNew.gvlCtx.getVarType(fbInstanceName);
                if(fbTypeName == null) fbTypeName = fbInstanceName;
                List<String> paramNames = new ArrayList<>();
                List<String> paramValues = new ArrayList<>();
                for(PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()){
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    paramNames.add(plcVariable.getName());
                    paramValues.add(translatorNew.gvlCtx.translateExpr(plcVariable.getAssignVar()));
                }
                sb.append(translatorNew.gvlCtx.emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues));
            }else{
                PLCVariable funcSymbol = (PLCVariable) firstSym;
                for (PLCSTPARSERParser.Param_assignContext param_assignContext : childCtx.param_assign()) {
                    PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "call parameter");
                    String typeName = plcVariable.getRuntimeTypeName();
                    if (typeName == null || typeName.isEmpty()) {
                        typeName = "INT";
                    }
                    typeName = translatorNew.gvlCtx.toNativeType(typeName);
                    sb.append("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + translatorNew.gvlCtx.translateExpr(plcVariable.getAssignVar()) + ";");
                }
                String var = translatorNew.gvlCtx.translateExpr(funcSymbol.getAssignVar()).substring(1);
                sb.append("\n\t\t" + var + ";");
            }
        }else{
            String result = translatorNew.visit(ctx.getChild(0));
            for (String decl : translatorNew.pendingDecls) {
                sb.append(decl);
            }
            translatorNew.pendingDecls.clear();
            if (result != null) {
                sb.append(result);
            }
        }


        return sb.toString();
    }
}
