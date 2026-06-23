package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCRefVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_ref_addr;

@StrategyForVisit(ruleIndex = RULE_ref_addr)
public class VisitRef_addr implements Strategy {

    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Ref_addrContext ctx  = (PLCSTPARSERParser.Ref_addrContext) parserCtx;
        PLCVariable referredVar = (PLCVariable) visitor.visit(ctx.getChild(2)).get(0);
        PLCRefVariable targetVar = new PLCRefVariable();
        targetVar.setAssignVar("&" + referredVar.getName());
        targetVar.setReferredVariable(referredVar);
        return visitor.packSymbols(targetVar);
    }
}
