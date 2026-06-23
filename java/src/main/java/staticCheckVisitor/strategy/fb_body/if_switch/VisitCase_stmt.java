package staticCheckVisitor.strategy.fb_body.if_switch;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_case_stmt;


@StrategyForVisit(ruleIndex = RULE_case_stmt)
public class VisitCase_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Case_stmtContext ctx = (PLCSTPARSERParser.Case_stmtContext) parserCtx;
        PLCSymbol exprSymbol = visitor.visit(ctx.expression()).get(0);
        PLCModifierEnum.Sort exprSort = exprSymbol.getSort();
        //switch只能使用int
        try{
            if(exprSort != PLCModifierEnum.Sort.INT){
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