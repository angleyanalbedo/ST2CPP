package staticCheckVisitor.strategy.fb_body;

import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_assert_stmt;

@StrategyForVisit(ruleIndex = RULE_assert_stmt)
public class VisitAssert_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Assert_stmtContext ctx = (PLCSTPARSERParser.Assert_stmtContext) parserCtx;
        visitor.visit(ctx.expression());
        return null;
    }
}
