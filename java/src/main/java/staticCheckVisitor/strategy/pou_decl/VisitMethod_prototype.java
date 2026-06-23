package staticCheckVisitor.strategy.pou_decl;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCMethodDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_method_prototype;

@StrategyForVisit(ruleIndex = RULE_method_prototype)
public class VisitMethod_prototype implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Method_prototypeContext ctx = (PLCSTPARSERParser.Method_prototypeContext) parserCtx;
        PLCMethodDeclSymbol methodDeclSymbol = new PLCMethodDeclSymbol();

        //method_name
        methodDeclSymbol.setName(ctx.method_name().getText());
        methodDeclSymbol.setRuntimeName("*" + methodDeclSymbol.getName() + "::callFunc");

        //分配符号id和类型id
        methodDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        methodDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(methodDeclSymbol);

        //入栈
        PLCScopeStack.push(methodDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(methodDeclSymbol);
        PLCTotalSymbolTable.addBlock(methodDeclSymbol);

        //data_type_access
        //获得直接返回值类型和分类
        if(ctx.data_type_access() != null){
            PLCTypeDeclSymbol typedVar = (PLCTypeDeclSymbol) visitor.visit(ctx.data_type_access()).get(0);
            methodDeclSymbol.setReturnTypeId(typedVar.getTypeId());
            methodDeclSymbol.setReturnVarSort(typedVar.getVarSort());
        }

        //io_var_decls
        for (PLCSTPARSERParser.Io_var_declsContext io_var_decl : ctx.io_var_decls()) {
            ArrayList<PLCSymbol> symbols = visitor.visit(io_var_decl);
            for (PLCSymbol symbol : symbols) {
                PLCVariable var = (PLCVariable) symbol;
                methodDeclSymbol.addAccessVar(var);
            }
        }

        //出栈
        PLCScopeStack.pop();
        return visitor.packSymbols(methodDeclSymbol);
    }
}