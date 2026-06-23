package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCRefVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_ref_deref;
@StrategyForVisit(ruleIndex = RULE_ref_deref)
public class VisitRef_deref implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Ref_derefContext ctx  = (PLCSTPARSERParser.Ref_derefContext) parserCtx;
            PLCRefVariable refVarName = new PLCRefVariable();
            String name = ctx.getChild(0).getText();
            refVarName.setName(name);
            int referredLevel = ctx.getChildCount()-1;
            refVarName.setReferredLevel(referredLevel);
            return visitor.packSymbols(refVarName);
    }
}
