package staticCheckVisitor;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;

public class GenerateBasicTypes {
    public void generate(){
        addBasicSymbol(IDGenerator.BOOL, "BOOL", PLCModifierEnum.Sort.BOOL, "(*(new BOOL(false)))", "BOOL",
                new int[]{},
                new int[]{},
                new int[]{IDGenerator.BOOL},
                new int[]{IDGenerator.BOOL});

        addBasicSymbol(IDGenerator.SINTID, "SINT", PLCModifierEnum.Sort.INT, "(*(new SINT(0)))", "SINT",
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL});
        addBasicSymbol(IDGenerator.INTID, "INT", PLCModifierEnum.Sort.INT, "(*(new INT(0)))", "INT",
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL});

        addBasicSymbol(IDGenerator.DINTID, "DINT", PLCModifierEnum.Sort.INT, "(*(new DINT(0)))", "DINT",
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL});

        addBasicSymbol(IDGenerator.LINTID, "LINT", PLCModifierEnum.Sort.INT, "(*(new LINT(0)))", "LINT",
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL});

        addBasicSymbol(IDGenerator.REAL, "REAL", PLCModifierEnum.Sort.REAL, "(*(new REAL(0)))", "REAL",
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL},
                new int[]{IDGenerator.SINTID, IDGenerator.INTID, IDGenerator.DINTID, IDGenerator.LINTID, IDGenerator.REAL});

        addBasicSymbol(IDGenerator.SSTRING, "STRING", PLCModifierEnum.Sort.STRING, "(*(new STRING(\"\")))", "STRING",
                new int[]{},
                new int[]{},
                new int[]{IDGenerator.SSTRING},
                new int[]{IDGenerator.SSTRING});
        //添加基本类型

    }

    //基本类型的typeid和符号id用同一个
    private void addBasicSymbol(int id,
                                       String name,
                                       PLCModifierEnum.Sort sort,
                                       String initVar,
                                       String runtimeName,
                                       int[] calcList,
                                       int[] compareList,
                                       int[] equalList,
                                       int[] assignList){

        PLCTypeDeclSymbol basicSymbol = new PLCTypeDeclSymbol(id, id, name);
        basicSymbol.setSort(sort);
        basicSymbol.setVarSort(sort);
        basicSymbol.setInitVar(initVar);
        basicSymbol.setRuntimeName(runtimeName);

        for (int typeId : calcList) {
            basicSymbol.addCalculableType(typeId);
        }

        for (int typeId : compareList) {
            basicSymbol.addComparableType(typeId);
        }

        for (int typeId : equalList) {
            basicSymbol.addEqualType(typeId);
        }

        for (int typeId : assignList) {
            basicSymbol.addAssignableType(typeId);
        }

        //加入总符号表
        PLCTotalSymbolTable.addSymbol(basicSymbol);
        PLCTotalSymbolTable.addType(basicSymbol);

        //加入基本符号表
        PLCScopeStack.basicTypeTable.addSymbol(basicSymbol);
    }
}
