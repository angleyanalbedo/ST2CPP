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

import static antlr4.PLCSTPARSERParser.RULE_compare_expr;


@StrategyForVisit(ruleIndex = RULE_compare_expr)
public class VisitCompare_expr implements Strategy {
    /**
     * 处理类似 visitExpression
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Compare_exprContext ctx = (PLCSTPARSERParser.Compare_exprContext) parserCtx;
        if(ctx.equ_expr().size() == 1){ //只有一个节点时，不理会Compare expr的类型，直接访问返回
            return visitor.visit(ctx.getChild(0));
        }
        else{  //有多个节点时，严格要求Compare expr类型兼容  类的多态使用类中重构的checkcanequalwith处理
            ArrayList<PLCSymbol> exprList = new ArrayList<>();
            //访问下层节点，将符号存在exprList
            for (PLCSTPARSERParser.Equ_exprContext equ_exprContext : ctx.equ_expr()) {
                exprList.add(visitor.visit(equ_exprContext).get(0));
            }
            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(exprList.get(0).getTypeId());

            StringBuilder compareVar = new StringBuilder();
            int index = 1;
            boolean ifConst = true;
            for (PLCSymbol symbol : exprList) {
                PLCVariable termSymbol = (PLCVariable) symbol;
                if(!termSymbol.getIfConst()){
                    ifConst = false;
                }
                int typeId = symbol.getTypeId();
                try{
                    if(!symbolType.checkCanEqualWith(typeId)){
                        throw new PLCSemanticException("type mismatch : " + ctx.getText());
                    }
                    symbolType = PLCTotalSymbolTable.getTypeByTypeID(typeId);
                }catch (PLCSemanticException e){
                    System.err.println(e.getMessage());
                    throw new RuntimeException(e);
                }

                //拼接字符串
                compareVar.append("(").append(termSymbol.getAssignVar()).append(") ");

                if(index < ctx.getChildCount()){
                    String opStr = ctx.getChild(index).getText();
                    if(opStr.equals("=")){
                        compareVar.append("==");
                    }else if (opStr.equals("<>")){
                        compareVar.append("!=");
                    }
                    index += 2;
                }
            }

            //检查未发现错误，新建一个表示此层类型的符号，返回给上层
            PLCVariable boolSymbol = new PLCVariable();
            boolSymbol.setTypeId(IDGenerator.BOOL);
            boolSymbol.setSort(PLCModifierEnum.Sort.BOOL);
            boolSymbol.setAssignVar(new String(compareVar));
            boolSymbol.setIfConst(ifConst);
            return visitor.packSymbols(boolSymbol);
        }
    }
}