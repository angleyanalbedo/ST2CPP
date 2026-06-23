package staticCheckVisitor.strategy.fb_body;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_stmt;

@StrategyForVisit(ruleIndex = RULE_stmt)
public class visitStmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.StmtContext ctx = (PLCSTPARSERParser.StmtContext) parserCtx;
        visitor.visit(ctx.getChild(0));
        return null;
    }
}