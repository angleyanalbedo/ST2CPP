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
import static antlr4.PLCSTPARSERParser.*;

@StrategyForVisit(ruleIndex = RULE_output_decls)
public class VisitOutput_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Output_declsContext ctx = (PLCSTPARSERParser.Output_declsContext)parserCtx;

        PLCVariable inputVarInfo = new PLCVariable();
        //section
        inputVarInfo.setVarSections(PLCModifierEnum.VarSections.VAR_OUTPUT);
        //retain
        if(ctx.RETAINORNONRETAIN() != null){
            inputVarInfo.setRetainQualifiers(visitor.visitorTool.getRetain(ctx.RETAINORNONRETAIN().getText()));
        }
        //output_decl
        ArrayList<PLCSymbol> vars = new ArrayList<>();
        //获得变量
        for(PLCSTPARSERParser.Output_declContext output_declContext : ctx.output_decl()){
            vars.addAll(visitor.visit(output_declContext));
        }

        //在此层组装信息 在decl层加入符号表
        for (PLCSymbol var : vars) {
            visitor.visitorTool.settleVarAttrs(inputVarInfo, (PLCVariable) var);
        }
        return vars;
    }
}
