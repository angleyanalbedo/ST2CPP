package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSubrangeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_subrange;

@StrategyForVisit(ruleIndex = RULE_subrange)
public class VisitSubrange implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.SubrangeContext ctx = (PLCSTPARSERParser.SubrangeContext) parserCtx;
        PLCVariable leftRange =(PLCVariable) visitor.visit(ctx.constant_expr(0)).get(0);
        PLCVariable rightRange =(PLCVariable) visitor.visit(ctx.constant_expr(0)).get(0);
        return visitor.packSymbols(leftRange, rightRange);
    }
}
