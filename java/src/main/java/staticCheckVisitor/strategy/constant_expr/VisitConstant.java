package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_constant;


@StrategyForVisit(ruleIndex = RULE_constant)
public class VisitConstant implements Strategy {
    /**
     * 返回子节点返回值
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.ConstantContext ctx = (PLCSTPARSERParser.ConstantContext) parserCtx;
        PLCVariable constSymbol = (PLCVariable) visitor.visit(ctx.getChild(0)).get(0);
        constSymbol.setIfConst(true);
        return visitor.packSymbols(constSymbol);
    }
}