package PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.CallFunc;

import PLCSymbolAndScope.PLCSymbols.PLCBaseFUNDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCFBCallSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERParser;

import java.util.ArrayList;
import java.util.List;

public class TranslateFunc_call {
    public String translateNode(PLCSTPARSERParser.Func_callContext ctx, PLCTranslatorNew translatorNew){
        StringBuilder sb = new StringBuilder();
        // 先访问所有 param_assign 子节点，捕获嵌套 func_call 的临时变量声明
        StringBuilder nestedDecls = new StringBuilder();
        for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
            String childResult = translatorNew.visit(param_assignContext);
            if(childResult != null && !childResult.isEmpty()){
                nestedDecls.append(childResult);
            }
        }

        PLCSymbol firstSym = PLCTranslatorNew.getSymbol(ctx, "function call");
        if(firstSym instanceof PLCFBCallSymbol fbCallSym){
            String fbInstanceName = fbCallSym.getFbInstanceName();
            String fbTypeName = translatorNew.gvlCtx.getVarType(fbInstanceName);
            if(fbTypeName == null) fbTypeName = fbInstanceName;
            List<String> paramNames = new ArrayList<>();
            List<String> paramValues = new ArrayList<>();
            for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
                PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "function parameter");
                paramNames.add(plcVariable.getName());
                paramValues.add(translatorNew.gvlCtx.translateExpr(plcVariable.getAssignVar()));
            }
            sb.append(translatorNew.gvlCtx.emitFBCall(fbInstanceName, fbTypeName, paramNames, paramValues));
        }else{
            for(PLCSTPARSERParser.Param_assignContext param_assignContext : ctx.param_assign()){
                PLCVariable plcVariable = PLCTranslatorNew.getVariable(param_assignContext, "function parameter");
                // runtimeTypeName 在语义分析阶段可能未设置，使用 typeId 查询类型声明
                String typeName = plcVariable.getRuntimeTypeName();
                if(typeName == null || typeName.isEmpty() || "INT".equals(typeName)){
                    int tid = plcVariable.getTypeId();
                    if(tid != 0){
                        PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol tdecl =
                            PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable.getTypeByTypeID(tid);
                        if(tdecl != null && tdecl.getName() != null){
                            typeName = tdecl.getName();
                        }
                    }
                }
                if(typeName == null || typeName.isEmpty()){
                    typeName = "INT";
                }
                typeName = translatorNew.gvlCtx.toNativeType(typeName);
                sb.append("\n\t\t" + typeName + " " + plcVariable.getRuntimeName() + "=" + translatorNew.gvlCtx.translateExpr(plcVariable.getAssignVar()) + ";");
            }
        }
        // 将嵌套临时变量声明插入到本函数调用所有代码之前
        sb.insert(0, nestedDecls.toString());
        return sb.toString();
    }
}
