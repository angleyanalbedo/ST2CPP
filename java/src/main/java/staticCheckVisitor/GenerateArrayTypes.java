package staticCheckVisitor;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;

import java.util.ArrayList;

/**
 * 为新类型新建1——15维对应的数组类型
 * */
public class GenerateArrayTypes {
    public static int MAX_DIMENSION = 15;

    public void generate(PLCTypeDeclSymbol source){
        ArrayList<PLCArrayDeclSymbol> arrays = new ArrayList<>();
        PLCTotalSymbolTable.arraySymbolMap.put(source.getSymbolId(), arrays);
        for(int i = 1; i <= 16; i++){
            //创建新的数组类符号
            PLCArrayDeclSymbol arrayDeclSymbol = new PLCArrayDeclSymbol();
            //设置类型id，符号id
            arrayDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());
            arrayDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            //设置内部元素变量
            arrayDeclSymbol.setElementTypeId(source.getTypeId());
            //设置维度
            arrayDeclSymbol.setDimension(i);
            //设置名称 _ARRAY_{ElementTypeId}_{Dimension}
            arrayDeclSymbol.setName("_ARRAY_" + source.getTypeId() + "_" + i);

            //加入符号表
            PLCTotalSymbolTable.totalSymbolMap.put(arrayDeclSymbol.getSymbolId(), arrayDeclSymbol);
            PLCTotalSymbolTable.totalTypeMap.put(arrayDeclSymbol.getTypeId(), arrayDeclSymbol);
            arrays.add(arrayDeclSymbol);
        }
    }
}
