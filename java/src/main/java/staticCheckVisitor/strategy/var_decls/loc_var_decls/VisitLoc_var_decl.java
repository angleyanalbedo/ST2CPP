package staticCheckVisitor.strategy.var_decls.loc_var_decls;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_loc_var_decl;

@StrategyForVisit(ruleIndex = RULE_loc_var_decl)
public class VisitLoc_var_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Loc_var_declContext ctx = (PLCSTPARSERParser.Loc_var_declContext) parserCtx;
        PLCVariable returnVar = new PLCVariable();
        //located_at
        String location = ctx.located_at().Direct_variable().getText();
        //variable_name?
        if(ctx.variable_name() != null){
            returnVar.setName(ctx.variable_name().getText());
        }else{
            returnVar.setName("_varAt" + location);
        }

        //带有类型信息、初始值的变量
        //loc_var_spec_init
        PLCVariable varInfo = (PLCVariable) visitor.visit(ctx.loc_var_spec_init()).get(0);

        //组装信息
        returnVar.setTypeId(varInfo.getTypeId());
        returnVar.setSort(varInfo.getSort());
        returnVar.setAssignVar(varInfo.getAssignVar());
        returnVar.setDeclSymbol(varInfo.getDeclSymbol());
        returnVar.setRuntimeTypeName(varInfo.getRuntimeTypeName());

        //检查名称
        visitor.visitorTool.checkNameOnly(PLCScopeStack.currentSymbolTable, returnVar.getName());

        //装入符号表
        //分配符号id
        returnVar.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        //加入当前符号表
        PLCScopeStack.currentSymbolTable.addSymbol(returnVar);
        //加入总表
        PLCTotalSymbolTable.addSymbol(returnVar);

        return visitor.packSymbols(returnVar);
    }
}