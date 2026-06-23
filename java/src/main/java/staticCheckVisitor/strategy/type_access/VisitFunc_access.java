package staticCheckVisitor.strategy.type_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static PLCSymbolAndScope.PLCScopeStack.globalScope;
import static antlr4.PLCSTPARSERParser.RULE_func_access;

@StrategyForVisit(ruleIndex = RULE_func_access)
public class VisitFunc_access implements Strategy {
    /**
     * 从命名空间或者类实例中寻找函数体
     * 返回fc的复制品，包含基本的类型信息和runtime type name
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Func_accessContext ctx = (PLCSTPARSERParser.Func_accessContext) parserCtx;
        PLCScope scope = currentScope;
        String funcName = ctx.func_name().getText();

        //收集含有名称信息的PLCSymbol, 含有名称信息和取引用信息
        ArrayList<PLCSymbol> scopeNameList = new ArrayList<>();
        for (PLCSTPARSERParser.Scope_nameContext nameContext : ctx.scope_name()) {
            scopeNameList.addAll(visitor.visit(nameContext));
        }

        //第一个节点为THIS， scope全为类或方法块的实例
        StringBuilder runtimeTypeName = new StringBuilder();

        if("THIS".equals(ctx.getChild(0).getText())){
            //检查当前作用域时什么的作用域,只能是方法块、方法
            PLCImportScopeTypeDeclType declSymbol = scope.getDeclSymbol();
            PLCModifierEnum.Sort currentSymbolSort = declSymbol.getSort();
            switch (currentSymbolSort){
                case METHOD_DECL -> {
                    scope = scope.getParentScope();
                }
                case FB_DECL -> {}
                default -> {
                    throw new PLCSemanticException("only use THIS in class and FB : " + ctx.getText());
                }
            }

            //寻找scope
            //组装runtimeTypeName
            for (PLCSymbol scopeSymbol : scopeNameList) {
                //检查是不是类或方法块的实例
                try{
                    //检查实例是否存在
                    String instanceName = scopeSymbol.getName();
                    //统一进行浅搜索
                    PLCVariable instance = (PLCVariable) scope.shallowFindSymbol(instanceName);
                    //检查是否是类或方法块的实例
                    PLCBaseClassDeclSymbol scopeLocateType = (PLCBaseClassDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(instance.getTypeId());
                    //切换当前作用域
                    scope = scopeLocateType.getImportScope();
                    //组装runtimeTypeName
                    runtimeTypeName.append(instanceName).append(".");
                }catch(ClassCastException e){
                    throw new PLCSemanticException("error scope : " + scopeSymbol.getName());
                }
            }
            PLCSymbol function = scope.shallowFindSymbol(funcName);
            if(function == null || function.getSort() != PLCModifierEnum.Sort.METHOD_DECL){
                throw new PLCSemanticException("can not find method : " + funcName + "  from : " + ctx.getText());
            }

            //添加函数名
            runtimeTypeName.append(function.getRuntimeName());
            //组装返回
            PLCBaseFUNDeclSymbol targetVar = new PLCBaseFUNDeclSymbol((PLCBaseFUNDeclSymbol) function);
            targetVar.setRuntimeTypeName(new String(runtimeTypeName));

            return visitor.packSymbols(targetVar);

        }else{ //A.B.METHOD();
            PLCBaseFUNDeclSymbol basicFunc = null;
            //找到scope
            if(!scopeNameList.isEmpty()){
                //检查第一个以确定是namespace嵌套还是实例, 进行深搜索
                PLCSymbol symbol = scopeNameList.get(0);
                String symbolName = symbol.getName();

                PLCModifierEnum.Sort[] sorts = {PLCModifierEnum.Sort.NAMESPACE_DECL, PLCModifierEnum.Sort.CLASS, PLCModifierEnum.Sort.FB};

                PLCSymbol importScopeSymbol = null;
                for (PLCModifierEnum.Sort sort : sorts) {
                    importScopeSymbol = scope.deepFindSymbol(symbolName, sort);
                    if(importScopeSymbol != null){
                        break;
                    }
                }
                //搜索不到则报错
                if(importScopeSymbol == null){
                    throw new PLCSemanticException("can not find " + symbolName + " from : " + ctx.getText());
                }
                //找到基准
                PLCModifierEnum.Sort basicSort = importScopeSymbol.getSort();

                //其余的按照基准不同进行处理
                switch (basicSort){
                    case NAMESPACE_DECL ->{  //全为np，进行深搜索
                        //获取scope
                        PLCNamespaceDeclSymbol np = (PLCNamespaceDeclSymbol) importScopeSymbol;
                        //组装
                        runtimeTypeName.append(np.getRuntimeName()).append(".");
                        scope = np.getImportScope();
                        for(int i=1; i <= scopeNameList.size()-1; i++){
                            String namespaceName = scopeNameList.get(i).getName();
                            PLCSymbol scopeLocation = scope.deepFindSymbol(namespaceName, PLCModifierEnum.Sort.NAMESPACE_DECL);
                            if(scopeLocation == null){
                                throw new PLCSemanticException("can not find namespace : " + namespaceName + "from : " + ctx.getText());
                            }
                            scope = ((PLCNamespaceDeclSymbol) scopeLocation).getImportScope();
                            //runtimeTypeName组装
                            runtimeTypeName.append(scopeLocation.getRuntimeName()).append(".");
                        }
                        PLCSymbol function = scope.deepFindSymbol(funcName, PLCModifierEnum.Sort.FC_DECL);
                        if(function == null){
                            throw new PLCSemanticException("can not find function : " + funcName + "from : " + ctx.getText());
                        }
                        basicFunc = (PLCBaseFUNDeclSymbol) function;
                    }
                    case CLASS, FB -> {  //全为class 或者 fb，进行浅搜索
                        //获取scope
                        PLCBaseClassDeclSymbol baseClassSymbolOfFirstScope = (PLCBaseClassDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(importScopeSymbol.getTypeId());
                        runtimeTypeName.append(baseClassSymbolOfFirstScope.getRuntimeName()).append(".");
                        scope = baseClassSymbolOfFirstScope.getImportScope();
                        ArrayList<PLCModifierEnum.Sort> validSorts = new ArrayList<>(Arrays.asList(PLCModifierEnum.Sort.CLASS , PLCModifierEnum.Sort.FB));
                        for(int i=1; i <= scopeNameList.size()-1; i++){
                            String baseClassInstanceName = scopeNameList.get(i).getName();
                            PLCSymbol scopeLocation = scope.shallowFindSymbol(baseClassInstanceName);
                            if(scopeLocation == null || !validSorts.contains(scopeLocation.getSort())){
                                throw new PLCSemanticException("can not find class instance : " + baseClassInstanceName + "from : " + ctx.getText());
                            }
                            PLCClassDeclSymbol baseClassTypeOfInstance = (PLCClassDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(scopeLocation.getTypeId());
                            scope = baseClassTypeOfInstance.getImportScope();

                            runtimeTypeName.append(scopeLocation.getRuntimeName()).append(".");
                        }
                        PLCSymbol function = scope.shallowFindSymbol(funcName, PLCModifierEnum.Sort.METHOD_DECL);
                        if(function == null){
                            throw new PLCSemanticException("can not find method : " + funcName + "from : " + ctx.getText());
                        }
                        basicFunc = (PLCBaseFUNDeclSymbol) function;
                    }
                }
            }else{//method(),直接在当前作用域进行深搜索
                PLCSymbol function = scope.deepFindSymbol(funcName);
                ArrayList<PLCModifierEnum.Sort> validSorts = new ArrayList<>(Arrays.asList(PLCModifierEnum.Sort.METHOD_DECL, PLCModifierEnum.Sort.FC_DECL));
                if(function == null || !validSorts.contains(function.getSort())){
                    throw new PLCSemanticException("can not find method or function : " + funcName + "  from : " + ctx.getText());
                }
                basicFunc = (PLCBaseFUNDeclSymbol) function;
            }


            if(basicFunc == null){
                throw new PLCSemanticException("can not locate function or method : " + funcName + " from : " + ctx.getText());
            }
            //添加函数名
            runtimeTypeName.append(basicFunc.getRuntimeName());
            //组装返回
            PLCBaseFUNDeclSymbol targetVar = new PLCBaseFUNDeclSymbol(basicFunc);
            targetVar.setRuntimeTypeName(new String(runtimeTypeName));

            return visitor.packSymbols(targetVar);
        }
    }
}
