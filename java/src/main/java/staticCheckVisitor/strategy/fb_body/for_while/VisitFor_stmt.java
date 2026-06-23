package staticCheckVisitor.strategy.fb_body.for_while;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_for_stmt;

@StrategyForVisit(ruleIndex = RULE_for_stmt)
public class VisitFor_stmt implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.For_stmtContext ctx = (PLCSTPARSERParser.For_stmtContext) parserCtx;
        try{
            //control var
            PLCSymbol ctrlSymbol = visitor.visit(ctx.control_variable()).get(0);
            if(ctrlSymbol == null){
                throw new PLCSemanticException("undefined variable" + ctx.control_variable().getText());
            }
            //检查控制变量类型
            int ctrlTypeId = ctrlSymbol.getTypeId();
            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(ctrlTypeId);
            if(!symbolType.checkCanMathCalcWith(IDGenerator.INTID)){
                throw new PLCSemanticException("type mismatch" + ctx.getText());
            }
            //expr
            for(int i=0; i<=1; ++i){
                PLCSymbol exprSymbol = visitor.visit(ctx.expression(i)).get(0);
                PLCModifierEnum.Sort sort = exprSymbol.getSort();
                if(sort != PLCModifierEnum.Sort.INT){
                    throw new PLCSemanticException("type mismatch" + ctx.getText());
                }
            }
            //by list
            if(ctx.by_list() != null){
                visitor.visit(ctx.by_list());
            }
            //stmt list
            visitor.visit(ctx.stmt_list());

        }catch(PLCSemanticException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }
}