package staticCheckVisitor.strategy.constant_expr;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_equ_expr;



@StrategyForVisit(ruleIndex = RULE_equ_expr)
public class VisitEqu_expr implements Strategy {
    /**
     * 处理同 visitCompare_expr
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Equ_exprContext ctx = (PLCSTPARSERParser.Equ_exprContext) parserCtx;
        if(ctx.add_expr().size() == 1){ //只有一个节点时，不理会add expr的类型，直接访问返回
            return visitor.visit(ctx.getChild(0));
        }
        else{  //有多个节点时，严格要求add expr类型可计算
            ArrayList<PLCSymbol> exprList = new ArrayList<>();
            for (PLCSTPARSERParser.Add_exprContext exprContext : ctx.add_expr()) {
                exprList.add(visitor.visit(exprContext).get(0));
            }

            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprList.get(0).getTypeId());

            StringBuilder equVar = new StringBuilder();
            int expressionSymbolIndex = 1;
            boolean ifConst = true;
            //循环获取add expr类型，查询是否可计算
            for (PLCSymbol symbol : exprList) {
                PLCVariable termSymbol = (PLCVariable) symbol;
                if(!termSymbol.getIfConst()){
                    ifConst = false;
                }
                int typeId = symbol.getTypeId();
                try{
                    if(!symbolType.checkCanCompareWith(typeId)){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }
                }catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    throw new RuntimeException(e);
                }
                equVar.append("(").append(termSymbol.getAssignVar()).append(") ");
                if(expressionSymbolIndex < ctx.getChildCount()){
                    equVar.append(ctx.getChild(expressionSymbolIndex).getText());
                    expressionSymbolIndex += 2;
                }
            }


            //检查未发现错误，新建一个表示此层类型的符号，返回给上层
            PLCVariable boolSymbol = new PLCVariable();
            boolSymbol.setTypeId(IDGenerator.BOOL);
            boolSymbol.setSort(PLCModifierEnum.Sort.BOOL);
            boolSymbol.setAssignVar(new String(equVar));
            boolSymbol.setIfConst(ifConst);
            return visitor.packSymbols(boolSymbol);
        }

    }
}