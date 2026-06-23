package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCSubrangeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_subrange_spec;

@StrategyForVisit(ruleIndex = RULE_subrange_spec)
public class VisitSubrange_spec implements Strategy {
    /**
     * 返回type decl symbol
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Subrange_specContext ctx = (PLCSTPARSERParser.Subrange_specContext) parserCtx;
        PLCSubrangeDeclSymbol plcSubrangeDeclSymbol;
        if (ctx.subrange()!=null){
            plcSubrangeDeclSymbol = new PLCSubrangeDeclSymbol();
            PLCVariable leftRange=(PLCVariable) visitor.visit(ctx.subrange()).get(0);
            PLCVariable rightRange=(PLCVariable) visitor.visit(ctx.subrange()).get(0);
            plcSubrangeDeclSymbol.setLowerLimit(leftRange.getAssignVar());
            plcSubrangeDeclSymbol.setUpperLimit(rightRange.getAssignVar());
            String subTypeName = ctx.int_type_name().getText();
            plcSubrangeDeclSymbol.setSubTypeId(PLCScopeStack.basicTypeTable.findSymbol(subTypeName).getTypeId());
            plcSubrangeDeclSymbol.setTypeId(-1);
        }
        else{
            plcSubrangeDeclSymbol = (PLCSubrangeDeclSymbol) visitor.visit(ctx.subrange_type_access()).get(0);
        }
        return visitor.packSymbols(plcSubrangeDeclSymbol);
    }
}