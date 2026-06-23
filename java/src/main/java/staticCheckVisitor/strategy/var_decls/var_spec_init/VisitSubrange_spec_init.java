package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_subrange_spec_init;

@StrategyForVisit(ruleIndex = RULE_subrange_spec_init)
public class VisitSubrange_spec_init implements Strategy {
    /**
     * 返回只拥有decl symbol initVar 的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Subrange_spec_initContext ctx = (PLCSTPARSERParser.Subrange_spec_initContext) parserCtx;
        //获取类型信息
        PLCTypeDeclSymbol typeSymbol = (PLCTypeDeclSymbol) visitor.visit(ctx.subrange_spec()).get(0);

        PLCVariable initVar = new PLCVariable();
        initVar.setDeclSymbol(typeSymbol);

        if(ctx.signed_int() != null){
            //获取表达式信息
            initVar.setAssignVar(ctx.signed_int().getText());
        }

        //返回包含类型id和初始值信息的一个符号
        return visitor.packSymbols(initVar);
    }
}
