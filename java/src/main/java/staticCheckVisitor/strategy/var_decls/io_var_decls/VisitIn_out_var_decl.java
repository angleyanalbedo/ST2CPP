package staticCheckVisitor.strategy.var_decls.io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_in_out_var_decl;

@StrategyForVisit(ruleIndex = RULE_in_out_var_decl)
public class VisitIn_out_var_decl implements Strategy {
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.In_out_var_declContext ctx = (PLCSTPARSERParser.In_out_var_declContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}
