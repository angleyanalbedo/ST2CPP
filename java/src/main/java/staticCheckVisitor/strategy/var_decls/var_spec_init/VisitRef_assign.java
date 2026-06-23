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

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_ref_assign;

@StrategyForVisit(ruleIndex = RULE_ref_assign)
public class VisitRef_assign implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Ref_assignContext ctx = (PLCSTPARSERParser.Ref_assignContext) parserCtx;
        String name = ctx.ref_name().toString();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);
        PLCRefVariable plcRefVariable = new PLCRefVariable();
        if (ctx.ref_value()!=null){
            plcRefVariable=(PLCRefVariable) visitor.visit(ctx.getChild(3)).get(0);
        }
        else {
            plcRefVariable.setAssignVar(ctx.getChild(3).getText());
        }
        plcRefVariable.setName(name);
        return visitor.packSymbols(plcRefVariable);
    }
}
