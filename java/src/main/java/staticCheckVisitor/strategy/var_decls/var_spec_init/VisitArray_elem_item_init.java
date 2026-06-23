package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_elem_item_init;

@StrategyForVisit(ruleIndex = RULE_array_elem_item_init)
public class VisitArray_elem_item_init implements Strategy {
    /**
     * 返回拥有assignVar的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_elem_item_initContext ctx = (PLCSTPARSERParser.Array_elem_item_initContext) parserCtx;
        StringBuilder initStr = new StringBuilder();
        for (PLCSTPARSERParser.Array_elem_init_valueContext valueContext : ctx.array_elem_init_value()) {
            PLCVariable symbol = (PLCVariable) visitor.visit(valueContext).get(0);
            initStr.append(symbol.getAssignVar()).append(", ");
        }
        String timeStr = ctx.Unsigned_int().getText().replace("_", "");
        int time = Integer.parseInt(timeStr);
        String repeatStr = new String(initStr);

        PLCVariable variable = new PLCVariable();
        variable.setAssignVar(repeatStr.repeat(time));
        return visitor.packSymbols(variable);
    }
}
