package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_numeric_literal;


@StrategyForVisit(ruleIndex = RULE_numeric_literal)
public class VisitNumeric_literal implements Strategy {
    /**
     * 返回子节点返回值
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Numeric_literalContext ctx = (PLCSTPARSERParser.Numeric_literalContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}