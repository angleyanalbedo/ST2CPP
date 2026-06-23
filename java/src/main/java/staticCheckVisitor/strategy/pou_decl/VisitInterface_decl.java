package staticCheckVisitor.strategy.pou_decl;

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
import static antlr4.PLCSTPARSERParser.RULE_interface_decl;

@StrategyForVisit(ruleIndex = RULE_interface_decl)
public class VisitInterface_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Interface_declContext ctx = (PLCSTPARSERParser.Interface_declContext) parserCtx;

        //检查重名
        String name = ctx.interface_type_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建Symbol
        PLCInterfaceDeclSymbol interfaceDeclSymbol = new PLCInterfaceDeclSymbol();
        interfaceDeclSymbol.setName(ctx.interface_type_name().getText());
        interfaceDeclSymbol.setRuntimeName(interfaceDeclSymbol.getName());

        //分配符号id和类型id
        interfaceDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        interfaceDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(interfaceDeclSymbol);

        //入栈
        PLCScopeStack.push(interfaceDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(interfaceDeclSymbol);
        PLCTotalSymbolTable.addBlock(interfaceDeclSymbol);

        //using_directive
        for (PLCSTPARSERParser.Using_directiveContext directiveContext : ctx.using_directive()) {
            ArrayList<PLCSymbol> namespaceSymbols = visitor.visit(directiveContext);
            PLCScope importScope = interfaceDeclSymbol.getImportScope();
            for (PLCSymbol symbol : namespaceSymbols) {
                PLCNamespaceDeclSymbol baseNp = (PLCNamespaceDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(symbol.getTypeId());
                importScope.addUsingNamespace(baseNp);
                interfaceDeclSymbol.addNameSpace(baseNp);
            }
        }

        //interface_name_list
        if(ctx.interface_name_list() != null){
            ArrayList<PLCSymbol> interfaceList = visitor.visit(ctx.interface_name_list());
            for (PLCSymbol symbol : interfaceList) {
                //接口的复制类
                PLCInterfaceDeclSymbol extendSymbol = (PLCInterfaceDeclSymbol) symbol;

                //接口的本体
                PLCInterfaceDeclSymbol baseInterface = (PLCInterfaceDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(interfaceDeclSymbol.getTypeId());
                baseInterface.addAssignableType(interfaceDeclSymbol.getTypeId());
                interfaceDeclSymbol.addInterface(baseInterface);
                interfaceDeclSymbol.addAllAbsMethods(baseInterface.getAbstractMethods());
            }
        }

        for (PLCSTPARSERParser.Method_prototypeContext method_prototypeContext : ctx.method_prototype()) {
            ArrayList<PLCSymbol> methods = visitor.visit(method_prototypeContext);
            interfaceDeclSymbol.addAbstractMethod((PLCMethodDeclSymbol) methods.get(0));
        }

        //出栈
        PLCScopeStack.pop();
        return null;
    }
}