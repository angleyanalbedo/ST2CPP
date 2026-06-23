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

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_struct_elem_init;

@StrategyForVisit(ruleIndex = RULE_struct_elem_init)
public class VisitStruct_elem_init implements Strategy{
    /**
     * 返回一个命名的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_elem_initContext ctx = (PLCSTPARSERParser.Struct_elem_initContext) parserCtx;
        String name = ctx.struct_elem_name().getText();
        PLCVariable structElemValue = (PLCVariable) visitor.visit(ctx.getChild(3)).get(0);
        structElemValue.setName(name);
        return visitor.packSymbols(structElemValue);
    }
}
