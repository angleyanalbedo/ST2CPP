package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_case_list_elem;

@StrategyForVisit(ruleIndex = RULE_case_list_elem)
public class VisitCase_list_elem implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Case_list_elemContext ctx = (PLCSTPARSERParser.Case_list_elemContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}