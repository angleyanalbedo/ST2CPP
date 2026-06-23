package staticCheckVisitor.strategy.type_access;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_class_type_access;
import static antlr4.PLCSTPARSERParser.RULE_interface_name_list;

@StrategyForVisit(ruleIndex = RULE_interface_name_list)
public class VisitInterface_name_list implements Strategy {
    /**
     * @describe 打包返回，在下一层进行检查
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Interface_name_listContext ctx = (PLCSTPARSERParser.Interface_name_listContext) parserCtx;
        ArrayList<PLCSymbol> interfaceList = new ArrayList<>();
        for (PLCSTPARSERParser.Interface_type_accessContext interfaceTypeAccess : ctx.interface_type_access()) {
            interfaceList.addAll(visitor.visit(interfaceTypeAccess));
        }
        return interfaceList;
    }
}