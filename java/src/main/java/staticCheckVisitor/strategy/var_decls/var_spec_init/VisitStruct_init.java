package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_struct_init;

@StrategyForVisit(ruleIndex = RULE_struct_init)
public class VisitStruct_init implements Strategy {
    /**
     * 返回一个PLCVariable，内部的信息保存在此var的DeclSymbol中
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_initContext ctx = (PLCSTPARSERParser.Struct_initContext) parserCtx;
        PLCStructDeclSymbol initStruct = new PLCStructDeclSymbol();
        PLCVariable structInitVar = new PLCVariable();
        for (PLCSTPARSERParser.Struct_elem_initContext struct_elem_initContext : ctx.struct_elem_init()) {
            PLCVariable structElem = (PLCVariable) visitor.visit(struct_elem_initContext).get(0);
            initStruct.addVariable(structElem);
        }
        structInitVar.setDeclSymbol(initStruct);
        structInitVar.setSort(PLCModifierEnum.Sort.STRUCT);
        return visitor.packSymbols(structInitVar);
    }
}