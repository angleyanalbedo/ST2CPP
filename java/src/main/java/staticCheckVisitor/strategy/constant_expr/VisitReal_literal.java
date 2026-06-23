package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
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

import static antlr4.PLCSTPARSERParser.RULE_real_literal;

@StrategyForVisit(ruleIndex = RULE_real_literal)
public class VisitReal_literal implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Real_literalContext ctx = (PLCSTPARSERParser.Real_literalContext) parserCtx;
        if(ctx.Real_Type_Name() != null){
            //获得强制转换的类型
            PLCTypeDeclSymbol forceTranslateType = (PLCTypeDeclSymbol) PLCScopeStack.basicTypeTable.findSymbol(ctx.Real_Type_Name().getText());

            PLCVariable realSymbol = new PLCVariable();
            realSymbol.setTypeId(forceTranslateType.getTypeId());
            realSymbol.setSort(PLCModifierEnum.Sort.REAL);
            StringBuilder realVar = new StringBuilder();
            realVar.append("(*(new "). append(forceTranslateType.getRuntimeName()).append("(");
            for(int i=2; i <= ctx.getChildCount()-1; i++){
                realVar.append(ctx.getChild(i).getText());
            }
            realVar.append(")))");
            realSymbol.setAssignVar(new String(realVar));

            return visitor.packSymbols(realSymbol);
        }else{
            PLCVariable realSymbol = new PLCVariable();
            realSymbol.setTypeId(IDGenerator.REAL);
            realSymbol.setSort(PLCModifierEnum.Sort.REAL);
            realSymbol.setAssignVar("(*(new REAL(" + ctx.getText() + ")))");
            return visitor.packSymbols(realSymbol);
        }

    }
}