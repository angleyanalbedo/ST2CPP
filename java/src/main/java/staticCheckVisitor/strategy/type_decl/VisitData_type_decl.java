package staticCheckVisitor.strategy.type_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_data_type_decl;

@StrategyForVisit(ruleIndex = RULE_data_type_decl)
public class VisitData_type_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Data_type_declContext ctx = (PLCSTPARSERParser.Data_type_declContext) parserCtx;
        for (PLCSTPARSERParser.Type_declContext declContext : ctx.type_decl()) {
            visitor.visit(declContext);
        }
        return null;
    }
}
