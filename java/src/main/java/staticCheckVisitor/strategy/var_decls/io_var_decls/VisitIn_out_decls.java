package staticCheckVisitor.strategy.var_decls.io_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_in_out_decls;

@StrategyForVisit(ruleIndex = RULE_in_out_decls)
public class VisitIn_out_decls implements Strategy {
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.In_out_declsContext ctx = (PLCSTPARSERParser.In_out_declsContext) parserCtx;
        PLCVariable inputVarInfo = new PLCVariable();
        //section
        inputVarInfo.setVarSections(PLCModifierEnum.VarSections.VAR_IN_OUT);

        //in_out_put_decl
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        //获得变量
        if(ctx.in_out_var_decl().size() > 0){
            for(PLCSTPARSERParser.In_out_var_declContext in_out_var_declContext : ctx.in_out_var_decl()){
                vars.addAll(visitor.visit(in_out_var_declContext));
            }
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(inputVarInfo, (PLCVariable) var);
        }
        return vars;
    }
}
