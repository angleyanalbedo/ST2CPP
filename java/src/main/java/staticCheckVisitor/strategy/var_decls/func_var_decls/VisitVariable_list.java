package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_var_decl_init;
import static antlr4.PLCSTPARSERParser.RULE_variable_list;

@StrategyForVisit(ruleIndex = RULE_variable_list)
public class VisitVariable_list  implements Strategy {
    /**
     * @describe 返回只拥有名称信息的PLCVariable s
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Variable_listContext ctx = (PLCSTPARSERParser.Variable_listContext) parserCtx;
        //返回该行定义的一个或多个符号变量的名称信息
        ArrayList<PLCSymbol> variableNameList = new ArrayList<>();

        //访问Variable_name获得名称
        for(PLCSTPARSERParser.Variable_nameContext nameContext : ctx.variable_name()){
            PLCSymbol namedSymbol = visitor.visit(nameContext).get(0);
            variableNameList.add(namedSymbol);
        }
        return variableNameList;
    }
    }
