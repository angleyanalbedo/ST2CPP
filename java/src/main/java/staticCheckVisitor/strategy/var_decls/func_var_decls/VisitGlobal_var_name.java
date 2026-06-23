package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_elem_type_name;
import static antlr4.PLCSTPARSERParser.RULE_global_var_name;

@StrategyForVisit(ruleIndex = RULE_global_var_name)
public class VisitGlobal_var_name  implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Global_var_nameContext ctx = (PLCSTPARSERParser.Global_var_nameContext) parserCtx;
        PLCVariable namedVar = new PLCVariable();
        namedVar.setName(ctx.identifier().getText());
        return visitor.packSymbols(namedVar);
    }
}