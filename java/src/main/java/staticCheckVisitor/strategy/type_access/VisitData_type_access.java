package staticCheckVisitor.strategy.type_access;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_data_type_access;
import static antlr4.PLCSTPARSERParser.RULE_loc_var_decls;

@StrategyForVisit(ruleIndex = RULE_data_type_access)
public class VisitData_type_access implements Strategy {
    /**
     * 类型信息 默认初始值 runtimename，runtime type name 返回类型为PLCTypeDeclSymbol
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Data_type_accessContext ctx = (PLCSTPARSERParser.Data_type_accessContext) parserCtx;
        return visitor.visit(ctx.getChild(0));
    }
}