package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_else_stmt;

@StrategyForVisit(ruleIndex = RULE_else_stmt)
public class VisitElse_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Else_stmtContext ctx = (PLCSTPARSERParser.Else_stmtContext) parserCtx;
        visitor.visit(ctx.stmt_list());
        return null;
    }
}