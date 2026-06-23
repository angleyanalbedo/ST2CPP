package staticCheckVisitor;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCImportScopeTypeDeclType;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;

import java.util.ArrayList;
import java.util.HashSet;

public class VisitorTool {
    private static final VisitorTool visitorTool = new VisitorTool();

    private VisitorTool(){
        //注册可以进行加减乘除乘方取模运算的符号id
        registerCalculableType(IDGenerator.INTID);
        registerCalculableType(IDGenerator.SINTID);
        registerCalculableType(IDGenerator.DINTID);
        registerCalculableType(IDGenerator.LINTID);
    }

    //可以进行计算的类型
    private final HashSet<Integer> calculableTypeIdSet = new HashSet<>();

    @Deprecated
    private void registerCalculableType(int typeId){
        this.calculableTypeIdSet.add(typeId);
    }

    /**
     * 使用PLCTypeDeclSymbol类下的检查方法
     * */
    @Deprecated
    public boolean checkIfCalculable(int type){
        return !this.calculableTypeIdSet.contains(type);
    }

    /**
     *  检查两个类型是否兼容((比较、赋值、表达式))(不处理类)
     * 使用PLCTypeDeclSymbol类下的检查方法
     * */
    @Deprecated
    public boolean checkCompatibility(int leftType, int rightType){
        if(leftType == rightType){
            return false;
        }
        else{
            return checkIfCalculable(leftType) || checkIfCalculable(rightType);
        }
    }

    /*-----------变量信息整合-------------------------------------------*/

    /**
     * @describe 将resource中收集到的变量信息(VarSections、AccessModifie、IfConst、RetainQualifiers)赋给target
     * */
    public void settleVarAttrs(PLCVariable resource, PLCVariable target){
        target.setVarSections(resource.getVarSections());
        target.setAccessModifier(resource.getAccessModifier());
        target.setIfConst(resource.getIfConst());
        target.setRetainQualifiers(resource.getRetainQualifiers());
//        target.setLocation(resource.getLocation());
    }

    /**
     * @describe  在当前符号表内查询是否存在重名符号，若存在则抛出错误
     * */
    public void checkNameOnly(PLCSymbolTable checkSymbolTable, String symbolName){
        try{
            PLCSymbol foundSymbol = checkSymbolTable.findSymbol(symbolName);
            if(foundSymbol != null){
                throw new PLCSemanticException("duplication of name:" + symbolName +
                        " duplicate symbol info : " + foundSymbol);
            }
        }
        catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //变量声明处理
    /**
     * @describe symbolTable唯一名称检查->将varinfo中的变量信息整合到var->为varList中的变量设置id->添加到符号表symbolTable以及总表
     * */
    public void processVariable(ArrayList<PLCSymbol> varList, PLCVariable infoVar, PLCSymbolTable symbolTable){
        for(PLCSymbol symbol : varList){
            //名称检查
            String symbolName = symbol.getName();
            checkNameOnly(symbolTable, symbolName);
            //组装信息
            PLCVariable targetSymbol = (PLCVariable)symbol;
            settleVarAttrs(infoVar, targetSymbol);
            //分配符号id
            targetSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            //加入当前符号表
            symbolTable.addSymbol(targetSymbol);
            //加入总表
            PLCTotalSymbolTable.addSymbol(targetSymbol);
        }
    }

    /**
     * @describe 获得工具对象
     * */
    public static VisitorTool getTool(){
        return visitorTool;
    }


    /**
     * @describe 根据字符串获得访问类型
     * */
    public PLCModifierEnum.AccessModifier getAccessType(String accessContext){
        switch (accessContext){
            case "PRIVATE":
                return PLCModifierEnum.AccessModifier.PRIVATE;
            case "PUBLIC":
                return PLCModifierEnum.AccessModifier.PUBLIC;
            case "INTERNAL":
                return PLCModifierEnum.AccessModifier.INTERNAL;
            case "PROTECTED":
                return PLCModifierEnum.AccessModifier.PROTECTED;
            default:
                return null;
        }
    }

    /**
     * @describe 根据字符串获得保留类型
     * */
    public PLCModifierEnum.RetainModifier getRetain(String retainText){
        switch(retainText){
            case "RETAIN" :
                return PLCModifierEnum.RetainModifier.RETAIN;
            case "NON_RETAIN":
                return PLCModifierEnum.RetainModifier.NON_RETAIN;
            default:
                return null;
        }
    }

    /**
     * @describe 依据名称序列寻找对应的命名空间作用域
     * 传入只有名称信息的namespace符号
     * */
    public PLCScope findNestedNameSpaceScope(ArrayList<PLCSymbol> nestNamespaceList) {
        //nestList为空，则返回null
        if (nestNamespaceList.isEmpty()) {
            return null;
        }
        PLCScope searchingScope = PLCScopeStack.currentScope;
        PLCSymbol targetSymbol;
        for(PLCSymbol namespaceSymbol : nestNamespaceList){
            targetSymbol = searchingScope.deepFindSymbol(namespaceSymbol.name, PLCModifierEnum.Sort.NAMESPACE_DECL);
            //查找不到，返回null
            if(targetSymbol == null){
                return null;
            }
            PLCImportScopeTypeDeclType tempSymbol = (PLCImportScopeTypeDeclType)targetSymbol;
            searchingScope = tempSymbol.getImportScope();
        }
        return searchingScope;
    }

    public PLCScope findNameSpaceScopeByNames(ArrayList<String> nameSpaceNames) {
        if(nameSpaceNames.isEmpty()){
            return null;
        }
        PLCScope searchingScope = PLCScopeStack.currentScope;
        PLCSymbol targetSymbol;
        for(String npName : nameSpaceNames){
            targetSymbol = searchingScope.deepFindSymbol(npName, PLCModifierEnum.Sort.NAMESPACE_DECL);
            //查找不到，返回null
            if(targetSymbol == null){
                return null;
            }
            PLCImportScopeTypeDeclType tempSymbol = (PLCImportScopeTypeDeclType)targetSymbol;
            searchingScope = tempSymbol.getImportScope();
        }
        return searchingScope;
    }
}
