package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_if_stmt;

@StrategyForVisit(ruleIndex = RULE_if_stmt)
public class VisitIf_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.If_stmtContext ctx = (PLCSTPARSERParser.If_stmtContext) parserCtx;
        PLCSymbol exprSymbol  = visitor.visit(ctx.expression()).get(0);
        int exprTypeId = exprSymbol.getTypeId();
        try {
            if (exprTypeId != IDGenerator.BOOL) {
                throw new PLCSemanticException("type mismatch" + ctx.getText());
            }
        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        for(int i=1; i<=ctx.getChildCount()-1; ++i){
            visitor.visit(ctx.getChild(i));
        }

        return null;
    }
}