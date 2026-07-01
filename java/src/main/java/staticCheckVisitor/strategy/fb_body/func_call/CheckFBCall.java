package staticCheckVisitor.strategy.fb_body.func_call;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;

import java.util.ArrayList;

public class CheckFBCall {

    public PLCFBCallSymbol checkFBCall(PLCFBCallSymbol fbCallSym, ArrayList<PLCVariable> params) {
        PLCFBDeclSymbol fbDecl = (PLCFBDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(fbCallSym.getFbTypeId());
        PLCSymbolTable fbTable = fbDecl.getImportSymbolTable();

        // 收集 FB 的所有参数（不管 varSection）
        java.util.HashMap<String, PLCVariable> allParams = new java.util.HashMap<>();
        for(PLCSymbol sym : fbTable.getSymbolIDHashMap().values()){
            if(sym instanceof PLCVariable pv){
                allParams.put(pv.getName(), pv);
            }
        }

        java.util.HashMap<String, String> paramMap = new java.util.HashMap<>();
        for (PLCVariable param : params) {
            String paramName = param.getName();
            if (paramName == null) {
                throw new PLCSemanticException("Irregular calls are not supported");
            }
            PLCVariable accessVar = allParams.get(paramName);
            if (accessVar == null) {
                throw new PLCSemanticException(paramName + " param name error: " + paramName);
            }
            PLCTypeDeclSymbol varType = PLCTotalSymbolTable.getTypeByTypeID(accessVar.getTypeId());
            if (!varType.checkCanAssignWith(param.getTypeId())) {
                throw new PLCSemanticException("type mismatch for param " + paramName);
            }
            paramMap.put(paramName, param.getRuntimeName());
            fbCallSym.addInputParam(param);
        }

        // 为未提供的输入参数填入默认值
        for (PLCVariable accessVar : allParams.values()) {
            String accessVarName = accessVar.getName();
            if (!paramMap.containsKey(accessVarName)) {
                String defaultAssign = accessVar.getAssignVar();
                if (defaultAssign != null && !defaultAssign.isEmpty()) {
                    fbCallSym.addInputParam(accessVar);
                }
            }
        }

        return fbCallSym;
    }
}
