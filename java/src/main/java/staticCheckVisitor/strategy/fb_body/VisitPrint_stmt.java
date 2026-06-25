package staticCheckVisitor.strategy.fb_body;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_print_stmt;

@StrategyForVisit(ruleIndex = RULE_print_stmt)
public class VisitPrint_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Print_stmtContext ctx = (PLCSTPARSERParser.Print_stmtContext) parserCtx;
        for (PLCSTPARSERParser.Print_stmt_elementContext element : ctx.print_stmt_element()) {
            visitor.visit(element);
        }
        return null;
    }
}
