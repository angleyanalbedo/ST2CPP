package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_init;

@StrategyForVisit(ruleIndex = RULE_array_init)
public class VisitArray_init implements Strategy {
    /**
     * 返回拥有assignVar的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_initContext ctx = (PLCSTPARSERParser.Array_initContext) parserCtx;
        StringBuilder initVar = new StringBuilder();
        initVar.append("{");
        for (PLCSTPARSERParser.Array_elem_initContext context : ctx.array_elem_init()) {
            PLCVariable initElem = (PLCVariable) visitor.visit(context).get(0);
            initVar.append("{").append(initElem.getAssignVar()).append("}");
        }
        initVar.append("}");
        PLCVariable initVariable = new PLCVariable();
        initVariable.setAssignVar(new String(initVar));
        initVariable.setSort(PLCModifierEnum.Sort.ARRAY);
        return visitor.packSymbols(initVariable);
    }
}
