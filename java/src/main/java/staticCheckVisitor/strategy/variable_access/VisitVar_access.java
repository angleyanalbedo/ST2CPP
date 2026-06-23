package staticCheckVisitor.strategy.variable_access;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCRefVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_var_access;

@StrategyForVisit(ruleIndex = RULE_var_access)
public class VisitVar_access implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Var_accessContext ctx = (PLCSTPARSERParser.Var_accessContext) parserCtx;
        PLCRefVariable plcRefVariable = new PLCRefVariable();
        String name = ctx.getChild(0).getChild(0).getText();
        plcRefVariable.setName(name);
        if (ctx.ref_deref()!=null) {
            PLCRefVariable refDeref = (PLCRefVariable) visitor.visit(ctx.ref_deref()).get(0);
            plcRefVariable.setReferredLevel(refDeref.getReferredLevel());
            plcRefVariable.setSort(PLCModifierEnum.Sort.REF);
        }

        return visitor.packSymbols(plcRefVariable);
    }
}
