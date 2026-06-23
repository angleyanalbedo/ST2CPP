package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_power_expr;
import static antlr4.PLCSTPARSERParser.RULE_primary_expr;

@StrategyForVisit(ruleIndex = RULE_primary_expr)
public class VisitPrimary_expr implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Primary_exprContext ctx = (PLCSTPARSERParser.Primary_exprContext) parserCtx;
        if(ctx.expression() != null){
            PLCVariable exprSymbol = (PLCVariable) visitor.visit(ctx.expression()).get(0);
            StringBuilder exprVar = new StringBuilder();
            exprVar.append("(").append(exprSymbol.getAssignVar()).append(")");
            exprSymbol.setAssignVar(new String(exprVar));
            return visitor.packSymbols(exprSymbol);
        }
        else{
            return visitor.visit(ctx.getChild(0));
        }
    }
}
