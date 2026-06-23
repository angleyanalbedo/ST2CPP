package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_loc_var_spec_init;

@StrategyForVisit(ruleIndex = RULE_loc_var_spec_init)
public class VisitLoc_var_spec_init implements Strategy {
    /**
     * 返回拥有初始值、typeid、sort runtimeTypeName的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        return visitor.visit(parserCtx.getChild(0));
    }
}
