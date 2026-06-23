package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_bool_literal;

@StrategyForVisit(ruleIndex = RULE_bool_literal)
public class VisitBool_literal implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Bool_literalContext ctx = (PLCSTPARSERParser.Bool_literalContext) parserCtx;
        PLCVariable plcSymbol = new PLCVariable();
        plcSymbol.setTypeId(IDGenerator.BOOL);
        plcSymbol.setSort(PLCModifierEnum.Sort.BOOL);
        String boolLit;
        if(ctx.Bool_Type_Name() != null){
            boolLit = ctx.getChild(ctx.getChildCount()-1).getText();
        }else {
            boolLit = ctx.getText();
        }

        StringBuilder assignVar = new StringBuilder();
        assignVar.append("(*").
                append("(new ").
                append("BOOL").
                append("(").
                append(boolLit).
                append(")").append(")").append(")");
        plcSymbol.setAssignVar(new String(assignVar));

        return visitor.packSymbols(plcSymbol);
    }
}