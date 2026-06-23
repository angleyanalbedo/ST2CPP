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

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_func_decl;

@StrategyForVisit(ruleIndex = RULE_func_decl)
public class VisitFunc_decl implements Strategy {

    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Func_declContext ctx = (PLCSTPARSERParser.Func_declContext) parserCtx;

        //检查重名
        String name = ctx.derived_func_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建func decl symbol
        PLCFCDeclSymbol fcSymbol = new PLCFCDeclSymbol();
        fcSymbol.setName(ctx.derived_func_name().getText());
        fcSymbol.setRuntimeName("*" + fcSymbol.getName() + "::callFunc");

        //分配符号id和类型id
        fcSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        fcSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(fcSymbol);

        //入栈
        PLCScopeStack.push(fcSymbol);

        //总表
        PLCTotalSymbolTable.addType(fcSymbol);
        PLCTotalSymbolTable.addBlock(fcSymbol);


        //获得直接返回值类型和分类
        if(ctx.data_type_access() != null){
            PLCTypeDeclSymbol typedVar = (PLCTypeDeclSymbol) visitor.visit(ctx.data_type_access()).get(0);
            fcSymbol.setReturnTypeId(typedVar.getTypeId());
            fcSymbol.setReturnVarSort(typedVar.getVarSort());
        }

        //设置方法使用的命名空间，命名空间的合法性在using_directive中检查
        for (PLCSTPARSERParser.Using_directiveContext using_directiveContext : ctx.using_directive()) {
            ArrayList<PLCSymbol> namespaceSymbols = visitor.visit(using_directiveContext);
            PLCScope importScope = fcSymbol.getImportScope();
            for (PLCSymbol symbol : namespaceSymbols) {
                PLCNamespaceDeclSymbol baseNp = (PLCNamespaceDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(symbol.getTypeId());
                importScope.addUsingNamespace(baseNp);
                fcSymbol.addNameSpace(baseNp);
            }
        }


        for(PLCSTPARSERParser.Io_var_declsContext declsContext : ctx.io_var_decls()){
            ArrayList<PLCSymbol> io_vars = visitor.visit(declsContext);
            for (PLCSymbol var : io_vars) {
                fcSymbol.addAccessVar((PLCVariable) var);
                fcSymbol.addVariable((PLCVariable) var);
            }
        }

        for(PLCSTPARSERParser.Func_var_declsContext declsContext : ctx.func_var_decls()){
            ArrayList<PLCSymbol> vars = visitor.visit(declsContext);
            for (PLCSymbol varSymbol : vars) {
                PLCVariable var = (PLCVariable) varSymbol;
                if(var.getVarSections() != PLCModifierEnum.VarSections.VAR_GLOBAL){
                    fcSymbol.addVariable(var);
                }
            }
        }

        for(PLCSTPARSERParser.Temp_var_declsContext declsContext : ctx.temp_var_decls()){
            ArrayList<PLCSymbol> vars = visitor.visit(declsContext);
            for (PLCSymbol var : vars) {
                fcSymbol.addVariable((PLCVariable) var);
            }
        }

        if(ctx.func_body() != null){
            visitor.visit(ctx.func_body());
        }

        if(fcSymbol.getReturnTypeId() != -1 && !fcSymbol.isIfReturned()){
            throw new PLCSemanticException("function " + fcSymbol.getName() + " needs return");
        }

        //--------------------------------------------------------------

        //将func作用域出栈
        PLCScopeStack.pop();
        return visitor.packSymbols(fcSymbol);
    }
}