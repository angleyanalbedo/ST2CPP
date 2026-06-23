package staticCheckVisitor.strategy.var_decls.io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_output_decl;

@StrategyForVisit(ruleIndex = RULE_output_decl)
public class VisitOutput_decl implements Strategy {
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Output_declContext ctx = (PLCSTPARSERParser.Output_declContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}
