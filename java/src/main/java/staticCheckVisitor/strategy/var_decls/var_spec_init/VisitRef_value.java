package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCRefVariable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_spec;
import static antlr4.PLCSTPARSERParser.RULE_ref_value;

@StrategyForVisit(ruleIndex = RULE_ref_value)
public class VisitRef_value implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        if(parserCtx.getChild(0).getText().equals("NULL")){
            PLCRefVariable refVar = new PLCRefVariable();
            refVar.setAssignVar("NULL");
            return visitor.packSymbols(refVar);
        }else{
            return visitor.visit(parserCtx.getChild(0));
        }
    }
}
