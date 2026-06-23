package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCStructDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;

import java.util.ArrayList;

public class CompareStructElements {
    /**
     * 检查结构体赋值是否正确
     * */
    public boolean compareStructElements(ArrayList<PLCVariable> basicVars, ArrayList<PLCVariable> comparingVars){
        if(basicVars.size() != comparingVars.size()){
            return false;
        }

        for (int i = 0; i < basicVars.size(); i++) {
            PLCVariable basicVar = basicVars.get(i);
            PLCVariable comparingVar = comparingVars.get(i);

            if(!basicVar.getName().equals(comparingVar.getName())){
                return false;
            }

            int varTypeId = basicVar.getTypeId();
            PLCTypeDeclSymbol varType = PLCTotalSymbolTable.getTypeByTypeID(varTypeId);

            int comparingVarTypeId = comparingVar.getTypeId();
            PLCTypeDeclSymbol comparingType = PLCTotalSymbolTable.getTypeByTypeID(comparingVarTypeId);

            if(varType.getSort() == PLCModifierEnum.Sort.STRUCT_DECL){
                if(comparingType.getSort() != PLCModifierEnum.Sort.STRUCT_DECL){
                    return false;
                }
                PLCStructDeclSymbol structVarType = (PLCStructDeclSymbol) varType;
                PLCStructDeclSymbol comparingStructType = (PLCStructDeclSymbol) comparingType;
                return this.compareStructElements(structVarType.getVariables(), comparingStructType.getVariables());
            }else if(basicVar.getSort() == PLCModifierEnum.Sort.ARRAY){
                if(comparingVar.getSort() != PLCModifierEnum.Sort.ARRAY){
                    return false;
                }
            }else{
                if(!varType.checkCanAssignWith(comparingVarTypeId)){
                    return false;
                }
            }
        }
        return true;
    }
}
