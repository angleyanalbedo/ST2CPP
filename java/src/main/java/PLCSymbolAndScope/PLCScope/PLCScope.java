package PLCSymbolAndScope.PLCScope;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCImportScopeTypeDeclType;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum.Sort;
import PLCSymbolAndScope.PLCSymbols.PLCNamespaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//ID、作用域对应符号、符号表引用不能为空（global作用域除外）
public class PLCScope {
    public String getScopeLocation() {
        return scopeLocation;
    }

    public void setScopeLocation(String scopeLocation) {
        this.scopeLocation = scopeLocation;
    }

    //作用域定位
    String scopeLocation = "";

    //引入此作用域的符号, 在入栈时自动设置
    private PLCImportScopeTypeDeclType declSymbol;

    //该作用域的符号表, 在入栈时自动设置
    private PLCSymbolTable scopeSymbolTable;

    //作用域唯一ID,在构造方法内初始化
    private int scopeID;

    //该作用域的父作用域,在构造方法内初始化
    private PLCScope parentScope;

    //当前作用域嵌套深度,在构造方法内初始化
    private int scopeDepth;

    //该作用域的子作用域,无需特地进行初始化，在声明子作用域时会自动添加
    public ArrayList<PLCScope> childScopeList = new ArrayList<>();

    /**
     * 该作用域使用的命名空间, 需要手动调用方法进行初始化
     * ----------子作用域会自动继承父作用域的using namespace------------------------
     */
    protected ArrayList<PLCNamespaceDeclSymbol> usingNSList = new ArrayList<>();

    /**
     * 由使用命名空间引入的有效搜索域 = 使用的命名空间本身 + 命名空间的validScopeFromNamespace
     * */
    protected ArrayList<PLCScope> validScopeFromNamespace = new ArrayList<>();

    /**
     * 由父作用域引入的有效搜索域 = 父作用域本身 + 父作用域的validScopeFromParents + 父作用域的validScopeFromNamespace
     * */
    protected ArrayList<PLCScope> validScopeFromParents = new ArrayList<>();


    //去除scope list中的重复元素
    private ArrayList<PLCScope> removeDuplication(ArrayList<PLCScope> scopeList){
        LinkedHashSet<PLCScope> listWithoutDuplicateElements = new LinkedHashSet<>(scopeList);
        return new ArrayList<>(listWithoutDuplicateElements);
    }

    public PLCScope(){
        this.scopeID = IDGenerator.getIDGenerator().newScopeId();
        PLCScope tempParentScope = PLCScopeStack.currentScope;
        //全局作用域没有父作用域, 与普通作用域区分对待
        if(tempParentScope != null){
            scopeDepth = tempParentScope.getScopeDepth() + 1;

            //设置父子关系
            this.parentScope = PLCScopeStack.currentScope;
            PLCScopeStack.currentScope.addChildScope(this);

            //拷贝父作用域的命名空间
            this.copyUsingNSList(this.parentScope);

            //添加有效作用域,并去除重复
            this.validScopeFromParents.add(this.parentScope);
            this.validScopeFromParents.addAll(this.parentScope.validScopeFromParents);
            this.validScopeFromParents.addAll(this.parentScope.validScopeFromNamespace);
            this.validScopeFromParents = removeDuplication(this.validScopeFromParents);
        }
        else{
            scopeDepth = 0;
        }
    }

    //添加子作用域
    private void addChildScope(PLCScope scope){
        this.childScopeList.add(scope);
    }

    //添加命名空间
    public void addUsingNamespace(PLCNamespaceDeclSymbol namespace){

        this.validScopeFromNamespace.add(namespace.getImportScope());
        this.validScopeFromNamespace.addAll(namespace.getImportScope().validScopeFromNamespace);
        this.validScopeFromNamespace = removeDuplication(this.validScopeFromNamespace);
        this.usingNSList.add(namespace);
    }


    //**********************************搜素方法***********************************************


    /**
     * 浅搜索
    * 仅仅在此作用域的符号表搜索符号
    * */
    public PLCSymbol shallowFindSymbol(String name){
        return this.scopeSymbolTable.findSymbol(name);
    }

    public PLCSymbol shallowFindSymbol(String name, Sort sort){
        return this.scopeSymbolTable.findSymbol(name, sort);
    }


    /**
     * 名称：深搜索
     * 浅搜索 -> 搜索validScopeFromParents -> 搜索validScopeFromNamespace
     * */
    public PLCSymbol deepFindSymbol(String name){
        //浅搜索
        PLCSymbol result = this.shallowFindSymbol(name);
        if(result != null){
            return result;
        }

        //搜索validScopeFromParents
        for(PLCScope scope : this.validScopeFromParents){
            result = scope.shallowFindSymbol(name);
            if(result != null){
                return result;
            }
        }

        //搜索搜索validScopeFromNamespace
        for(PLCScope scope : this.validScopeFromNamespace){
            result = scope.shallowFindSymbol(name);
            if(result != null){
                return result;
            }
        }
        return null;
    }

    /**
     * 返回所有同名的符号的数组，按照查找到的顺序排放*/
    public ArrayList<PLCSymbol> deepFindAllSymbols(String name){
        ArrayList<PLCSymbol> symbolArrayList = new ArrayList<>();
        //浅搜索
        PLCSymbol result = this.shallowFindSymbol(name);
        if(result != null){
            symbolArrayList.add(result);
        }

        //搜索validScopeFromParents
        for(PLCScope scope : this.validScopeFromParents){
            result = scope.shallowFindSymbol(name);
            if(result != null){
                symbolArrayList.add(result);
            }
        }

        //搜索搜索validScopeFromNamespace
        for(PLCScope scope : this.validScopeFromNamespace){
            result = scope.shallowFindSymbol(name);
            if(result != null){
                symbolArrayList.add(result);
            }
        }
        return symbolArrayList;
    }

    /**
     * 由特定条件搜索符号
     * */
    public PLCSymbol deepFindSymbol(String name, Sort sort){
        ArrayList<PLCSymbol> symbolArrayList = this.deepFindAllSymbols(name);
        for(PLCSymbol symbol : symbolArrayList){
            if(symbol.getSort() == sort){
                return symbol;
            }
        }
        return null;
    }


    //*********************************以下为set、get方法**************************************

    //引入此作用域的符号的获得和设置方法
    public void setDeclSymbol(PLCImportScopeTypeDeclType declSymbol) {
        this.declSymbol = declSymbol;
    }

    public PLCImportScopeTypeDeclType getDeclSymbol() {
        return declSymbol;
    }

    //该作用域符号表的设置和获得方法
    public void setScopeSymbolTable(PLCSymbolTable scopeSymbolTable) {
        this.scopeSymbolTable = scopeSymbolTable;
    }
    public PLCSymbolTable getScopeSymbolTable(){
        return this.scopeSymbolTable;
    }

    //嵌套深度的设置、获取方法
    public void setScopeDepth(int scopeDepth) {
        this.scopeDepth = scopeDepth;
    }

    public int getScopeDepth() {
        return scopeDepth;
    }

    //作用域唯一ID的设置、获取方法
    public void setScopeID(int scopeID) {
        this.scopeID = scopeID;
    }

    public int getScopeID() {
        return scopeID;
    }

    //父作用域
    public void setParentScope(PLCScope parentScope) {
        this.parentScope = parentScope;
    }

    public PLCScope getParentScope() {
        return parentScope;
    }

    //拷贝父作用域的命名空间
    private void copyUsingNSList(PLCScope another){
        this.usingNSList.addAll(another.usingNSList);
    }

    public String toString(){
        return "Declare symbol:" + this.declSymbol.name + "\n" +
                "Scope ID:" + this.scopeID + "\n" +
                "Scope depth:" + this.scopeDepth + "\n" +
                "Scope location: " + this.scopeLocation + "\n";

    }
}


