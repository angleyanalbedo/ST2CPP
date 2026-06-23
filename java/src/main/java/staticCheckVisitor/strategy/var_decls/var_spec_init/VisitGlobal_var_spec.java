package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_global_var_spec;

@StrategyForVisit(ruleIndex = RULE_global_var_spec)
public class VisitGlobal_var_spec implements Strategy {
    /**
     * return PLCVar with name and location
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Global_var_specContext ctx = (PLCSTPARSERParser.Global_var_specContext) parserCtx;
        if(ctx.located_at() == null){  //声明多个变量
            ArrayList<PLCSymbol> returnList = new ArrayList<>();
            for (PLCSTPARSERParser.Global_var_nameContext varNameContext : ctx.global_var_name()) {
                String varName = varNameContext.getText();
                PLCVariable var = new PLCVariable();
                var.setName(varName);
                returnList.add(var);
            }
            return returnList;
        }else{
            PLCVariable var = new PLCVariable();
            var.setName(ctx.global_var_name(0).getText());
            var.setLocation(ctx.located_at().Direct_variable().getText());
            return visitor.packSymbols(var);
        }
    }
}
