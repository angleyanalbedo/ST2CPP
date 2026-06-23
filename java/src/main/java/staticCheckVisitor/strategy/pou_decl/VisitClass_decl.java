package staticCheckVisitor.strategy.pou_decl;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
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

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_class_decl;

@StrategyForVisit(ruleIndex = RULE_class_decl)
public class VisitClass_decl implements Strategy {
    /**
     * @describe 返回组装完成的类声明符号
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Class_declContext ctx = (PLCSTPARSERParser.Class_declContext) parserCtx;

        //检查类的名称是否重复
        String className = ctx.class_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, className);

        //创建classSymbol
        PLCClassDeclSymbol classDeclSymbol = new PLCClassDeclSymbol();
        classDeclSymbol.setName(className);
        classDeclSymbol.setRuntimeName(className);

        //分配符号id和类型id
        classDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        classDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(classDeclSymbol);

        //入栈
        PLCScopeStack.push(classDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(classDeclSymbol);
        PLCTotalSymbolTable.addBlock(classDeclSymbol);

        //FINALORABSTRACT
        if(ctx.FINALORABSTRACT() != null){
            classDeclSymbol.setClassModifier(ctx.FINALORABSTRACT().getText());
        }

        //using_directive
        if(ctx.using_directive() != null){
            ArrayList<PLCSymbol> namespaceSymbols = visitor.visit(ctx.using_directive());
            PLCScope importScope = classDeclSymbol.getImportScope();
            for (PLCSymbol symbol : namespaceSymbols) {
                PLCNamespaceDeclSymbol baseNp = (PLCNamespaceDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(symbol.getTypeId());
                importScope.addUsingNamespace(baseNp);
                classDeclSymbol.addNameSpace(baseNp);
            }
        }

        //class_type_access
        if(ctx.class_type_access() != null){
            //基类的复制类
            PLCClassDeclSymbol extendClass = (PLCClassDeclSymbol)visitor.visit(ctx.class_type_access()).get(0);
            //基类的本体
            PLCBaseClassDeclSymbol baseClass = (PLCBaseClassDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(extendClass.getTypeId());
            baseClass.addAssignableType(classDeclSymbol.getTypeId());
            classDeclSymbol.setBaseClass(baseClass);
            //添加虚方法
            classDeclSymbol.addAllAbsMethods(baseClass.getAbstractMethods());
            //添加方法
            classDeclSymbol.addAllMethods(baseClass.getMethods());
            //添加变量
            classDeclSymbol.addAllVariable(baseClass.getVariableMap().values());
        }

        //interface_name_list
        if(ctx.interface_name_list() != null){
            ArrayList<PLCSymbol> interfaceList = visitor.visit(ctx.interface_name_list());
            for (PLCSymbol symbol : interfaceList) {
                //基类的复制类
                PLCInterfaceDeclSymbol interfaceDeclSymbol = (PLCInterfaceDeclSymbol) symbol;

                //接口的本体
                PLCInterfaceDeclSymbol baseInterface = (PLCInterfaceDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(interfaceDeclSymbol.getTypeId());
                baseInterface.addAssignableType(classDeclSymbol.getTypeId());
                classDeclSymbol.addInterface(baseInterface);
                //添加虚方法
                classDeclSymbol.addAllAbsMethods(baseInterface.getAbstractMethods());
            }
        }

        //func_var_decls
        for(PLCSTPARSERParser.Func_var_declsContext declsContext : ctx.func_var_decls()){
            ArrayList<PLCSymbol> vars = visitor.visit(declsContext);
            //添加符号
            for (PLCSymbol var : vars) {
                PLCVariable variable = (PLCVariable) var;
                classDeclSymbol.addVariable(variable);
            }
        }

        //other_var_decls
        for(PLCSTPARSERParser.Other_var_declsContext declsContext : ctx.other_var_decls()){
            ArrayList<PLCSymbol> vars = visitor.visit(declsContext);
            //添加符号
            for (PLCSymbol var : vars) {
                PLCVariable variable = (PLCVariable) var;
                //加入变量
                classDeclSymbol.addVariable(variable);
            }
        }

        //method_decl
        for (PLCSTPARSERParser.Method_declContext methodDeclContext : ctx.method_decl()) {
            PLCMethodDeclSymbol method = (PLCMethodDeclSymbol) visitor.visit(methodDeclContext).get(0);
            //加入方法
            classDeclSymbol.addMethod(method);
            if(method.getIfAbstract()){
                classDeclSymbol.addAbstractMethod(method);
            }
        }

        //检查虚函数实现
        try{
            ArrayList<PLCMethodDeclSymbol> abstractMethods = classDeclSymbol.getAbstractMethods();
            for (PLCMethodDeclSymbol method : abstractMethods) {
                String methodName = method.getName();
                PLCMethodDeclSymbol symbol = (PLCMethodDeclSymbol) currentSymbolTable.findSymbol(methodName, PLCModifierEnum.Sort.METHOD_DECL);
                if(symbol == null || !method.checkOverlap(symbol)){
                    throw new PLCSemanticException("Method " + methodName + " not implemented. FROM:" + ctx.getText());
                }
            }
        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }


        //出栈
        PLCScopeStack.pop();
        return visitor.packSymbols(classDeclSymbol);
    }
}