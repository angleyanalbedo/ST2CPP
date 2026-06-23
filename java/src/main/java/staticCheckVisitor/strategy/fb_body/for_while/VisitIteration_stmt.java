package staticCheckVisitor.strategy.fb_body.for_while;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_iteration_stmt;

@StrategyForVisit(ruleIndex = RULE_iteration_stmt)
public class VisitIteration_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Iteration_stmtContext ctx = (PLCSTPARSERParser.Iteration_stmtContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}