package staticCheckVisitor.strategy.fb_body.for_while;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_control_variable;


@StrategyForVisit(ruleIndex = RULE_control_variable)
public class VisitControl_variable implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Control_variableContext ctx = (PLCSTPARSERParser.Control_variableContext) parserCtx;
        String varName = ctx.identifier().getText();
        PLCSymbol varSymbol = currentScope.deepFindSymbol(varName);
        return visitor.packSymbols(varSymbol);
    }
}