package staticCheckVisitor.strategy.pou_decl;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_namespace_decl;
import static antlr4.PLCSTPARSERParser.RULE_namespace_elements;

@StrategyForVisit(ruleIndex = RULE_namespace_elements)
public class VisitNamespace_elements implements Strategy {
    /**
     * 访问所有子节点
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Namespace_elementsContext ctx = (PLCSTPARSERParser.Namespace_elementsContext) parserCtx;
        visitor.visitChildren(ctx);
        return null;
    }
}