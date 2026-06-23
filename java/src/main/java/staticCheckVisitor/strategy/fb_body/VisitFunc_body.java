package staticCheckVisitor.strategy.fb_body;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_func_body;

@StrategyForVisit(ruleIndex = RULE_func_body)
public class VisitFunc_body implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Func_bodyContext ctx = (PLCSTPARSERParser.Func_bodyContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}