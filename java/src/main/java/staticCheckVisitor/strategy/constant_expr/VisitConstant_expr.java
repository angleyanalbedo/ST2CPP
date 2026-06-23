package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_constant_expr;

@StrategyForVisit(ruleIndex = RULE_constant_expr)
public class VisitConstant_expr implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Constant_exprContext ctx = (PLCSTPARSERParser.Constant_exprContext) parserCtx;
        return visitor.visit(ctx.expression());
    }
}