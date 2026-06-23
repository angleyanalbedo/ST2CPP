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

import static antlr4.PLCSTPARSERParser.RULE_char_literal;


@StrategyForVisit(ruleIndex = RULE_char_literal)
public class VisitChar_literal implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Char_literalContext ctx = (PLCSTPARSERParser.Char_literalContext) parserCtx;
        PLCVariable plcSymbol = new PLCVariable();
        plcSymbol.setTypeId(IDGenerator.SSTRING);
        plcSymbol.setSort(PLCModifierEnum.Sort.STRING);

        StringBuilder assignVar = new StringBuilder();
        assignVar.append("(*").
                append("(new ").
                append("STRING").
                append("(").
                append(ctx.char_str().getText()).
                append(")").append(")").append(")");
        plcSymbol.setAssignVar(new String(assignVar));


        return visitor.packSymbols(plcSymbol);
    }
}