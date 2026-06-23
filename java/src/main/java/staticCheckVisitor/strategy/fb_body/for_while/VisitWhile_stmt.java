package staticCheckVisitor.strategy.fb_body.for_while;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_while_stmt;

@StrategyForVisit(ruleIndex = RULE_while_stmt)
public class VisitWhile_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.While_stmtContext ctx = (PLCSTPARSERParser.While_stmtContext) parserCtx;
        //expression
        PLCSymbol exprSymbol = visitor.visit(ctx.expression()).get(0);
        int exprTypeId = exprSymbol.getTypeId();
        try{
            if(exprTypeId != IDGenerator.BOOL){
                throw new PLCSemanticException("type mismatch" + ctx.getText());
            }
        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        //stmt_list
        visitor.visit(ctx.stmt_list());

        return null;

    }
}