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
import static antlr4.PLCSTPARSERParser.RULE_input_decls;

@StrategyForVisit(ruleIndex = RULE_input_decls)
public class VisitInput_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Input_declsContext ctx = (PLCSTPARSERParser.Input_declsContext) parserCtx;
        PLCVariable inputVarInfo = new PLCVariable();
        //section
        inputVarInfo.setVarSections(PLCModifierEnum.VarSections.VAR_INPUT);
        //retain
        if(ctx.RETAINORNONRETAIN() != null){
            inputVarInfo.setRetainQualifiers(visitor.visitorTool.getRetain(ctx.RETAINORNONRETAIN().getText()));
        }
        //input_decl
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        //获得变量
        for(PLCSTPARSERParser.Input_declContext input_declContext : ctx.input_decl()){
            vars.addAll(visitor.visit(input_declContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(inputVarInfo, (PLCVariable) var);
        }

        return vars;
    }
}
