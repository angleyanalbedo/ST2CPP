package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_int_literal;

@StrategyForVisit(ruleIndex = RULE_int_literal)
public class VisitInt_literal implements Strategy {
    /**
     * 返回类型id 为INT的符号
     * 存在针对值的强制转型，runtime支持后需要修改
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Int_literalContext ctx = (PLCSTPARSERParser.Int_literalContext) parserCtx;
        //存在强制转型
        if(ctx.int_type_name() != null){
            //获得强制转换的类型
            PLCTypeDeclSymbol forceTranslateType = (PLCTypeDeclSymbol) PLCScopeStack.basicTypeTable.findSymbol(ctx.int_type_name().getText());

            PLCVariable intSymbol = new PLCVariable();
            intSymbol.setTypeId(forceTranslateType.getTypeId());
            intSymbol.setSort(PLCModifierEnum.Sort.INT);
            String constNum = ctx.getChild(ctx.getChildCount()-1).getText();

            StringBuilder assignVar = new StringBuilder();
            assignVar.append("(*").
                    append("(new ").
                    append(forceTranslateType.getRuntimeName()).
                    append("(").
                    append(constNum).
                    append(")").append(")").append(")");
            intSymbol.setAssignVar(new String(assignVar));

//            intSymbol.setAssignVar( "new" + forceTranslateType.getRuntimeName() + "(" + constNum + ")");
            return visitor.packSymbols(intSymbol);
        }else { //不存在强制转型
            PLCVariable intSymbol = new PLCVariable();
            intSymbol.setTypeId(IDGenerator.INTID);
            intSymbol.setSort(PLCModifierEnum.Sort.INT);
            intSymbol.setAssignVar("(*(new INT(" + ctx.getChild(ctx.getChildCount()-1).getText() + ")))");
            return visitor.packSymbols(intSymbol);
        }

    }
}