package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_case_selection;

@StrategyForVisit(ruleIndex = RULE_case_selection)
public class VisitCase_selection implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Case_selectionContext ctx = (PLCSTPARSERParser.Case_selectionContext) parserCtx;
        visitor.visit(ctx.stmt_list());
        visitor.visit(ctx.case_list());
        return null;
    }
}