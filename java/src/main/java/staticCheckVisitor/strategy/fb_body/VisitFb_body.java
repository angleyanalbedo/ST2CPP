package staticCheckVisitor.strategy.fb_body;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_fb_body;

@StrategyForVisit(ruleIndex = RULE_fb_body)
public class VisitFb_body implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_bodyContext ctx = (PLCSTPARSERParser.Fb_bodyContext) parserCtx;
        visitor.visit(ctx.getChild(0));
        return null;
    }
}