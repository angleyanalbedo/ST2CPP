package staticCheckVisitor.strategy.fb_body;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_stmt_list;

@StrategyForVisit(ruleIndex = RULE_stmt_list)
public class VisitStmt_list implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Stmt_listContext ctx = (PLCSTPARSERParser.Stmt_listContext) parserCtx;
        if(ctx.stmt().size() != 0){
            for(PLCSTPARSERParser.StmtContext stmtContext : ctx.stmt()){
                visitor.visit(stmtContext);
            }
        }
        return null;
    }
}