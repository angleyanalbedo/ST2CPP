package staticCheckVisitor.strategy.variable_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_variable_access;

@StrategyForVisit(ruleIndex = RULE_variable_access)
public class VisitVariable_access implements Strategy {
    /**
     * 返回变量的复制品
     *
     * 机器相关的Location
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Variable_accessContext ctx = (PLCSTPARSERParser.Variable_accessContext) parserCtx;
        if(ctx.multibit_part_access() != null){ //地址
            PLCVariable variable = (PLCVariable) visitor.visit(ctx.variable()).get(0);
            String location = variable.getLocation();
            String subLocation = ctx.multibit_part_access().getText();
            if(location.isEmpty()){
                throw new PLCSemanticException("error locationl; from : " + ctx.getText());
            }
            variable.setLocation(location + subLocation);
            return visitor.packSymbols(variable);
        }else{  //只有variable
            return visitor.visit(ctx.variable());
        }
    }
}
