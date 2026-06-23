package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_selection_stmt;

@StrategyForVisit(ruleIndex = RULE_selection_stmt)
public class VisitSelection_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Selection_stmtContext ctx = (PLCSTPARSERParser.Selection_stmtContext) parserCtx;
        visitor.visit(ctx.getChild(0));
        return null;
    }
}