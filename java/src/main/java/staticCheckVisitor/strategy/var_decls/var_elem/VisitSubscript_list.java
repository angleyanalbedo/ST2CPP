package staticCheckVisitor.strategy.var_decls.var_elem;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_subscript_list;

@StrategyForVisit(ruleIndex = RULE_subscript_list)
public class VisitSubscript_list implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Subscript_listContext ctx = (PLCSTPARSERParser.Subscript_listContext) parserCtx;
        ArrayList<PLCSymbol> plcVariableArrayList = new ArrayList<>();
        for (PLCSTPARSERParser.SubscriptContext context : ctx.subscript()) {
            PLCVariable initElem = (PLCVariable) visitor.visit(context).get(0);
            plcVariableArrayList.add(initElem);
        }
        return plcVariableArrayList;
    }
}
