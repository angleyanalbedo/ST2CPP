package staticCheckVisitor.strategy.fb_body.func_call;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_func_call;
import static antlr4.PLCSTPARSERParser.RULE_subprog_ctrl_stmt;

@StrategyForVisit(ruleIndex = RULE_subprog_ctrl_stmt)
public class VisitCallFunc implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        return visitor.visit(parserCtx.getChild(0));
    }
}
