package staticCheckVisitor;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCRefDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;

import java.util.ArrayList;

public class GenerateRefTypes {
    public void generate(PLCTypeDeclSymbol source){
        //创建新的引用类符号
        PLCRefDeclSymbol refDeclSymbol = new PLCRefDeclSymbol();
        //设置类型id，符号id
        refDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());
        refDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        //设置内部元素id
        refDeclSymbol.setReferredTypeId(source.getTypeId());

        //设置名称 _REF_{ElementTypeId}_{Level}
        refDeclSymbol.setName("_REF_" + refDeclSymbol.getReferredTypeId());

        //加入符号表
        PLCTotalSymbolTable.totalSymbolMap.put(refDeclSymbol.getSymbolId(), refDeclSymbol);
        PLCTotalSymbolTable.totalTypeMap.put(refDeclSymbol.getTypeId(), refDeclSymbol);
        PLCTotalSymbolTable.refSymbolMap.put(source.getSymbolId(), refDeclSymbol);
    }
}
