package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_elem_init_value;

@StrategyForVisit(ruleIndex = RULE_array_elem_init_value)
public class VisitArray_elem_init_value implements Strategy {
    /**
     * 返回拥有assignVar的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        return visitor.visit(parserCtx.getChild(0));
    }
}
