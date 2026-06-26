package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.CallFunc;

import PLCSymbolAndScope.PLCSymbols.PLCFBCallSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateFunc_call {
    public String translateNode(PLCSTPARSERParser.Func_callContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        PLCSymbol firstSym = PLCTranslatorNew.getSymbol(ctx, "function call");
        if(firstSym instanceof PLCFBCallSymbol fbCallSym){
            String fbInstanceName = fbCallSym.getFbInstanceName();
            String fbTypeName = translatorNew.codeGen.getVarType(fbInstanceName);
            if(fbTypeName == null) fbTypeName = fbInstanceName;
            List<String> paramNames = new ArrayList<>();
            List<String> paramValues = new ArrayList<>();
            for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
                PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "function parameter");
                paramNames.add(plcVariable.getName());
                paramValues.add(translatorNew.codeGen.translateExpr(plcVariable.getAssignVar()));
            }
            sb.append(translatorNew.codeGen.emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues));
        }else{
            PLCVariable funcSymbol = (PLCVariable) firstSym;
            for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
                PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "function parameter");
                String typeName = plcVariable.getRuntimeTypeName();
                if(typeName == null || typeName.isEmpty()){
                    typeName = "INT";
                }
                typeName = translatorNew.codeGen.toNativeType(typeName);
                sb.append("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + translatorNew.codeGen.translateExpr(plcVariable.getAssignVar()) + ";");
            }
        }
        return sb.toString();
    }
}
