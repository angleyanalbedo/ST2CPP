package staticCheckVisitor.strategy.var_decls.func_var_decls;

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

import static antlr4.PLCSTPARSERParser.RULE_elem_type_name;
import static antlr4.PLCSTPARSERParser.RULE_external_decl;

@StrategyForVisit(ruleIndex = RULE_external_decl)
public class VisitExternal_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.External_declContext ctx = (PLCSTPARSERParser.External_declContext) parserCtx;

        PLCVariable varInfo = (PLCVariable) visitor.visit(ctx.getChild(2)).get(0);

        PLCVariable returnVar = new PLCVariable();

        returnVar.setName(ctx.global_var_name().getText());

        returnVar.setTypeId(varInfo.getTypeId());
        returnVar.setSort(varInfo.getSort());
        returnVar.setAssignVar(varInfo.getAssignVar());
        returnVar.setDeclSymbol(varInfo.getDeclSymbol());
        returnVar.setRuntimeTypeName(varInfo.getRuntimeTypeName());

        //检查名称
        visitor.visitorTool.checkNameOnly(PLCScopeStack.globalSymbolTable, returnVar.getName());

        //装入符号表
        //分配符号id
        returnVar.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        //加入当前符号表
        PLCScopeStack.globalSymbolTable.addSymbol(returnVar);
        //加入总表
        PLCTotalSymbolTable.addSymbol(returnVar);

        return visitor.packSymbols(returnVar);
    }
}
