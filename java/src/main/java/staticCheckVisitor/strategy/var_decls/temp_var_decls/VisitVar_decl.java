package staticCheckVisitor.strategy.var_decls.temp_var_decls;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_temp_var_decls;
import static antlr4.PLCSTPARSERParser.RULE_var_decl;

@StrategyForVisit(ruleIndex = RULE_var_decl)
public class VisitVar_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Var_declContext ctx = (PLCSTPARSERParser.Var_declContext) parserCtx;

        //访问variable_list获得只有变量名称的变量列表
        ArrayList<PLCSymbol> variableList = visitor.visit(ctx.variable_list());

        //访问第三个子节点（声明类型）获得变量类型信息(类型id)
        PLCVariable varInfo = (PLCVariable) visitor.visit(ctx.getChild(2)).get(0);

        ArrayList<PLCSymbol> returnList = new ArrayList<>();
        for (PLCSymbol var : variableList) {
            PLCVariable namedVar = (PLCVariable) var;
            PLCVariable returnVar = new PLCVariable();

            //组装信息
            returnVar.setName(namedVar.getName());
            returnVar.setLocation(namedVar.getLocation());

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

            returnList.add(returnVar);
        }

        return returnList;
    }
}
