package staticCheckVisitor.strategy.var_decls.var_elem;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_struct_elem_select;

@StrategyForVisit(ruleIndex = RULE_struct_elem_select)
public class VisitStruct_elem_select implements Strategy{
        @Override
        public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
            return visitor.visit(parserCtx.getChild(0));
    }
}
