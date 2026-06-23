package staticCheckVisitor.strategy.var_decls.other_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_loc_partly_var;

@StrategyForVisit(ruleIndex = RULE_loc_partly_var)
public class VisitLoc_partly_var implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Loc_partly_varContext ctx = (PLCSTPARSERParser.Loc_partly_varContext) parserCtx;
        PLCVariable var = new PLCVariable();
        //variable_name
        var.setName(ctx.variable_name().getText());
        //'AT' '%' ( 'I'| 'Q'| 'M') '*'
        StringBuilder location = new StringBuilder();
        location.append(ctx.getChild(2).getText()).append(ctx.getChild(3).getText()).append(ctx.getChild(4).getText());
        var.setLocation(new String(location));
        //var_spec
        PLCTypeDeclSymbol varType = (PLCTypeDeclSymbol) visitor.visit(ctx.var_spec()).get(0);
        var.setTypeId(varType.getTypeId());
        var.setSort(varType.getVarSort());
        var.setRuntimeTypeName(varType.getRuntimeTypeName());

        return visitor.packSymbols(var);
    }
}
