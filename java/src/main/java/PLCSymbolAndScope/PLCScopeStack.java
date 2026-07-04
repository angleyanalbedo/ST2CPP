package PLCSymbolAndScope;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCImportScopeTypeDeclType;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;

import java.util.Stack;

//作用域栈
public final class PLCScopeStack {
    //全局作用域,在创建实例后初始化，下面相同
    static public final PLCScope globalScope = new PLCScope();

    //全局的符号表
    static public final PLCSymbolTable globalSymbolTable = new PLCSymbolTable();

    //等价于this->top(),当前作用域的引用，本身没有实体，初始为全局作用域,在创建实例后初始化，下面相同
    static public PLCScope currentScope = null;

    //初始的符号表的引用,本身没有实体，默认为全局的符号表
    static public PLCSymbolTable currentSymbolTable = null;

    //基本类型符号表  查询基本类型用
    static public PLCSymbolTable basicTypeTable = new PLCSymbolTable();

    //作用域栈
    static final private Stack<PLCScope> scopeStack = new Stack<>();

    //初始化全局变量
    static public void stackInit(){
        if (!scopeStack.isEmpty()) return; // 已初始化
        //全局符号
        PLCImportScopeTypeDeclType globalSymbol = new PLCImportScopeTypeDeclType();
        globalSymbol.setName("GLOBAL");
        //压栈&设置引用关系
        push(globalSymbol, globalSymbolTable, globalScope);
        PLCTotalSymbolTable.addScope(globalScope);
        PLCTotalSymbolTable.addTable(globalSymbolTable);
    }

    // 重置：清空所有状态，重新初始化（测试用）
    // 安全调用：未初始化时也是安全的（getScopeSymbolTable() 可能返回 null）
    static public void reset() {
        scopeStack.clear();
        currentScope = null;
        currentSymbolTable = null;
        globalScope.childScopeList.clear();
        if (globalScope.getScopeSymbolTable() != null) {
            globalScope.getScopeSymbolTable().symbolNameHashMap.clear();
            globalScope.getScopeSymbolTable().symbolIDHashMap.clear();
        }
        basicTypeTable.symbolNameHashMap.clear();
        basicTypeTable.symbolIDHashMap.clear();
        PLCTotalSymbolTable.totalTypeMap.clear();
        PLCTotalSymbolTable.totalSymbolMap.clear();
        PLCTotalSymbolTable.totalScopeMap.clear();
        PLCTotalSymbolTable.totalTableMap.clear();
        PLCTotalSymbolTable.arraySymbolMap.clear();
        PLCTotalSymbolTable.refSymbolMap.clear();
        // stackInit 和 GenerateBasicTypes 由 PLCVisitor 构造函数调用
    }

    //设置符号、符号表、作用域之间的引用关系
    static private void setReference(PLCImportScopeTypeDeclType symbol, PLCSymbolTable table, PLCScope scope){
        scope.setDeclSymbol(symbol);
        scope.setScopeSymbolTable(table);
        table.setSrcSymbol(symbol);
        table.setTableScope(scope);
        symbol.setImportSymbolTable(table);
        symbol.setImportScope(scope);
    }

    //根据符号更新栈的内容,作用域和符号表在函数内创建,并设置引用关系,修改当前作用域, 并加入总表
    static public void push(PLCImportScopeTypeDeclType symbol){
        PLCScope newScope;
        PLCSymbolTable newTable;
        if(symbol.getImportScope() == null){
            //作用域和符号表在函数内创建,并设置引用关系
            newScope = new PLCScope();
            newTable = new PLCSymbolTable();
            setReference(symbol, newTable, newScope);
            //将符添加到栈顶作用域符号表中
            symbol.localScope.getScopeSymbolTable().addSymbol(symbol);
        }else {
            newScope = symbol.getImportScope();
            newTable = symbol.getImportSymbolTable();
        }

        if(newScope.getScopeDepth() >= 1){
            String instanceName;
            String name = symbol.getName();
            PLCModifierEnum.Sort sort  = symbol.getSort();
            switch (sort){
                case METHOD_DECL -> {
                    instanceName = "meth" + name;
                }
                case FC_DECL ->{
                    instanceName = "func" + name;
                }
                default -> instanceName = name;
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(newScope.getParentScope().getScopeLocation()).append("::").append(instanceName);
            newScope.setScopeLocation(new String(stringBuilder));
        }
        scopeStack.push(newScope);
        //修改当前作用域
        currentScope = newScope;
        currentSymbolTable = newTable;
    }

    //使用指定的符号 作用域和符号表向栈内添加新的符号, 并加入总表
    static public void push(PLCImportScopeTypeDeclType symbol, PLCSymbolTable table, PLCScope scope){
        //设置引用关系
        setReference(symbol, table, scope);
        scopeStack.push(scope);
        //修改当前作用域
        currentScope = scope;
        currentSymbolTable = table;
    }

    //弹出栈顶元素 并修改currentScope
    static public PLCScope pop(){
        try{
            if(currentScope == globalScope){
                throw new PLCSemanticException("pop global scope");
            }
        }
        catch(PLCSemanticException e){
            System.err.println(e.getMessage());
        }

        PLCScope tempScope = scopeStack.pop();
        //修改currentScope
        currentScope = scopeStack.peek();
        currentSymbolTable = currentScope.getScopeSymbolTable();
        return tempScope;
    }

    //返回栈顶元素
    static public PLCScope top(){
        return scopeStack.peek();
    }
}
