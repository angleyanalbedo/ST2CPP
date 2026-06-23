package staticCheckVisitor.strategy.fb_body.func_call;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCBaseFUNDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;

import java.util.ArrayList;
import java.util.HashMap;


public class CheckFuncCall {

    /**
     * 返回函数的调用信息
     * */
    public PLCVariable packageFuncCall(PLCBaseFUNDeclSymbol function, ArrayList<PLCVariable> params){
        //待返回的PLCVariable
        PLCVariable funcCallVar = new PLCVariable();

        //设置funcCallVar的type和sort信息
        int returnTypeId = function.getReturnTypeId();
        if(returnTypeId == -1){
            funcCallVar.setTypeId(-1);
            funcCallVar.setSort(function.getVarSort());
        }else{
            funcCallVar.setTypeId(returnTypeId);
            funcCallVar.setSort(PLCTotalSymbolTable.getTypeByTypeID(returnTypeId).getVarSort());
        }

        StringBuilder assignVar = new StringBuilder();
//        assignVar.append(function.getRuntimeName()).append("(");
        assignVar.append(function.getStdFunction()).append("(");
        HashMap<String, String> paramList = new HashMap<>();

        for (PLCVariable param : params) {
            String paramName = param.getName();
            paramList.put(paramName, param.getAssignVar());
        }

        //数量检查
        for (PLCVariable accessVar : function.getAccessVars()) {
            String accessVarName = accessVar.getName();
            String assignment = paramList.get(accessVarName);
            if(assignment == null){
                String defaultAssign = accessVar.getAssignVar();
                assignVar.append(defaultAssign);
            }else{
                assignVar.append(assignment);
            }
            assignVar.append(", ");
        }

        assignVar.delete(assignVar.length()-1, assignVar.length());
        assignVar.append(")");
        funcCallVar.setAssignVar(new String(assignVar));
        return funcCallVar;
    }


    /**
     *  在这里不检查访问权限
     *
     *  检查参数个数是否完整
     *  检查参数名称是否正确
     *  检查参数类型是否正确
     *  检查参数变量段是否正确
     *  暂时不支持不正规调用
     *  允许顺序错乱, 翻译出的会按照顺序排放
     * */
    public PLCVariable checkFuncCall(PLCBaseFUNDeclSymbol function, ArrayList<PLCVariable> params, String funcRuntimeTypeName){
        //待返回的PLCVariable
        PLCVariable funcCallVar = new PLCVariable();

        //设置funcCallVar的type和sort信息
        int returnTypeId = function.getReturnTypeId();
        if(returnTypeId == -1){
            funcCallVar.setTypeId(-1);
            funcCallVar.setSort(function.getVarSort());
        }else{
            funcCallVar.setTypeId(returnTypeId);
            funcCallVar.setSort(PLCTotalSymbolTable.getTypeByTypeID(returnTypeId).getVarSort());
        }

        StringBuilder assignVar = new StringBuilder();
        assignVar.append("*").append(function.getStdFunction()).append("(");

        //进行检查以及Assign Var
        try{
            HashMap<String, String> paramList = new HashMap<>();

            for (PLCVariable param : params) {
                String paramName = param.getName();
                if(paramName == null){
                    throw new PLCSemanticException("Irregular calls are not supported");
                }
                PLCVariable accessVar = function.getAccessVar(paramName);
                //名称检查
                if(accessVar == null){
                    throw new PLCSemanticException(paramName + " param name error or var section error: " + paramName);
                }

                //变量段检查
                PLCModifierEnum.VarSections paramSection = param.getVarSections();
                if(accessVar.getVarSections() != paramSection){
                    throw new PLCSemanticException(paramName + "var section is " + accessVar.getVarSections().toString()
                            + "but not " + paramSection.toString());
                }

                //类型检查
                PLCTypeDeclSymbol varType = PLCTotalSymbolTable.getTypeByTypeID(accessVar.getTypeId());
                if(!varType.checkCanAssignWith(param.getTypeId())){
                    throw new PLCSemanticException("type mismatch");
                }

                paramList.put(paramName, param.getRuntimeName());
            }


            //数量检查
            ArrayList<PLCVariable> accessVars = function.getAccessVars();
            for (PLCVariable accessVar : accessVars) {
                String accessVarName = accessVar.getName();
                String assignment = paramList.get(accessVarName);
                if(assignment == null){
                    String defaultAssign = accessVar.getAssignVar();
                    if(defaultAssign == null){
                        throw new PLCSemanticException("var " + accessVarName + "need param");
                    }else {
                        assignVar.append(" &").append(defaultAssign);
                    }
                }else{
                    assignVar.append("&").append(assignment);
                }
                assignVar.append(", ");
            }

            if(!accessVars.isEmpty()){
                assignVar.delete(assignVar.length()-2, assignVar.length());
            }

        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

        assignVar.append(")");

        funcCallVar.setAssignVar(new String(assignVar));

        funcCallVar.setRuntimeTypeName(funcRuntimeTypeName);

        return funcCallVar;
    }
}
