package staticCheckVisitor.strategy.type_access;

import PLCSymbolAndScope.PLCSymbols.PLCNamespaceDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_namespace_h_name;
import static antlr4.PLCSTPARSERParser.RULE_using_directive;

@StrategyForVisit(ruleIndex = RULE_namespace_h_name)
public class VisitNamespace_h_name implements Strategy {
    /**
     * @describe 返回命名空间名称序列
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Namespace_h_nameContext ctx = (PLCSTPARSERParser.Namespace_h_nameContext) parserCtx;
        ArrayList<PLCSymbol> nameList = new ArrayList<>();
        for (PLCSTPARSERParser.Namespace_nameContext nameContext : ctx.namespace_name()) {
            PLCNamespaceDeclSymbol symbol = new PLCNamespaceDeclSymbol();
            symbol.setName(nameContext.getText());
            nameList.add(symbol);
        }
        return nameList;
    }
}
