package staticCheckVisitor.strategy.fb_body.for_while;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_by_list;

@StrategyForVisit(ruleIndex = RULE_by_list)
public class VisitBy_list implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.By_listContext ctx = (PLCSTPARSERParser.By_listContext) parserCtx;
        PLCSymbol exprSymbol = visitor.visit(ctx.expression()).get(0);
        int exprTypeId = exprSymbol.getTypeId();
        PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprTypeId);
        try{
            if(!symbolType.checkCanMathCalcWith(IDGenerator.INTID)){
                throw new PLCSemanticException("type mismatch" + ctx.getText());
            }
        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return visitor.packSymbols(exprSymbol);
    }
}