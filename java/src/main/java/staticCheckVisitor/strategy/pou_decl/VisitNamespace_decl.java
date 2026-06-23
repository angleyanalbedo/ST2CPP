package staticCheckVisitor.strategy.pou_decl;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCInterfaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCNamespaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_namespace_decl;

@StrategyForVisit(ruleIndex = RULE_namespace_decl)
public class VisitNamespace_decl implements Strategy {
    /**
     * @describe 创建新的namespace或者添加到已存在的namespace中
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Namespace_declContext ctx = (PLCSTPARSERParser.Namespace_declContext) parserCtx;

        PLCNamespaceDeclSymbol namespaceDeclSymbol;

        ArrayList<PLCSymbol> namespaceName = visitor.visit(ctx.namespace_h_name());
        PLCScope namespaceScope = visitor.visitorTool.findNestedNameSpaceScope(namespaceName);
        //未找到namespace_h_name对应的命名空间，认定为创建新的命名空间
        if(namespaceScope == null){
            namespaceDeclSymbol = new PLCNamespaceDeclSymbol();
            namespaceDeclSymbol.setName(namespaceName.get(0).getName());
            namespaceDeclSymbol.setRuntimeName(namespaceDeclSymbol.getName());

            //分配符号id和类型id
            namespaceDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
            namespaceDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

            //加入符号表
            currentSymbolTable.addSymbol(namespaceDeclSymbol);

            //入栈
            PLCScopeStack.push(namespaceDeclSymbol);

            //总表
            PLCTotalSymbolTable.addType(namespaceDeclSymbol);
            PLCTotalSymbolTable.addBlock(namespaceDeclSymbol);
        }else{ //找到了认定为添加新的元素
            namespaceDeclSymbol = (PLCNamespaceDeclSymbol) namespaceScope.getDeclSymbol();
            PLCScopeStack.push(namespaceDeclSymbol);
        }



        //using_directive
        for (PLCSTPARSERParser.Using_directiveContext directiveContext : ctx.using_directive()) {
            ArrayList<PLCSymbol> namespaceSymbols = visitor.visit(directiveContext);
            PLCScope importScope = namespaceDeclSymbol.getImportScope();
            for (PLCSymbol symbol : namespaceSymbols) {
                PLCNamespaceDeclSymbol baseNp = (PLCNamespaceDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(symbol.getTypeId());
                importScope.addUsingNamespace(baseNp);
                namespaceDeclSymbol.addNameSpace(baseNp);
            }
        }

       visitor.visit(ctx.namespace_elements());

        //出栈
        PLCScopeStack.pop();
        return visitor.packSymbols(namespaceDeclSymbol);
    }
}