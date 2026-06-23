package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_derived_spec_init;

@StrategyForVisit(ruleIndex = RULE_derived_spec_init)
public class VisitDerived_spec_init implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Derived_spec_initContext ctx = (PLCSTPARSERParser.Derived_spec_initContext) parserCtx;
        if(ctx.elem_type_name()==null){
            return visitor.visit(ctx.getChild(0));
        }
        else{
            PLCVariable variable =(PLCVariable) visitor.visit(ctx.enum_spec_init()).get(0);
            PLCSymbol plcSymbol = visitor.visit(ctx.elem_type_name()).get(0);
            variable.setTypeId(plcSymbol.getTypeId());
            return visitor.packSymbols(variable);
        }
    }
}
