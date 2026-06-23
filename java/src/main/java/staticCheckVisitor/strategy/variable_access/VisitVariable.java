package staticCheckVisitor.strategy.variable_access;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_ref_addr;
import static antlr4.PLCSTPARSERParser.RULE_variable;

@StrategyForVisit(ruleIndex = RULE_variable)
public class VisitVariable implements Strategy {
    /**
     * 返回直接量类型或者symbolic_variable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.VariableContext ctx = (PLCSTPARSERParser.VariableContext) parserCtx;

        if(ctx.Direct_variable() != null){
            PLCVariable var = new PLCVariable();
            var.setLocation(ctx.Direct_variable().getText());
            return visitor.packSymbols(var);
        }else{
            return visitor.visit(ctx.symbolic_variable());
        }
    }
}
