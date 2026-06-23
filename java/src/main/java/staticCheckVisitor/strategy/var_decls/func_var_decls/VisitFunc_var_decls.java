package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_func_var_decls;

@StrategyForVisit(ruleIndex = RULE_func_var_decls)
public class VisitFunc_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Func_var_declsContext ctx = (PLCSTPARSERParser.Func_var_declsContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}
