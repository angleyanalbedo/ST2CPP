package staticCheckVisitor.strategy.var_decls.func_var_decls;

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

import static antlr4.PLCSTPARSERParser.RULE_var_decl_init;

@StrategyForVisit(ruleIndex = RULE_var_decl_init, branch = 0)
public class VisitVardeclinit  implements Strategy {
    /**
     *返回拥有变量名称 sort typeid 初始值 runtimeTypeName的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.VardeclinitContext ctx = (PLCSTPARSERParser.VardeclinitContext) parserCtx;
        //访问variable_list获得只有变量名称的变量列表
        ArrayList<PLCSymbol> variableList = visitor.visit(ctx.variable_list());

        //访问第三个子节点（声明类型）获得变量类型信息(类型id)
        //下层需要返回一个拥有类型信息typeid  sort、初始值信息 initvar的PLCVariable
        PLCVariable typeInfoSymbol = (PLCVariable) visitor.visit(ctx.getChild(2)).get(0);

        //组装获得变量类型
        ArrayList<PLCSymbol> returnList = new ArrayList<>();
        for(PLCSymbol targetSymbol : variableList){
            PLCVariable namedVar = (PLCVariable) targetSymbol;

            PLCVariable returnVar = new PLCVariable();

            //组装信息
            returnVar.setName(namedVar.getName());
            returnVar.setLocation(namedVar.getLocation());

            returnVar.setTypeId(typeInfoSymbol.getTypeId());
            returnVar.setSort(typeInfoSymbol.getSort());
            returnVar.setAssignVar(typeInfoSymbol.getAssignVar());
            returnVar.setDeclSymbol(typeInfoSymbol.getDeclSymbol());
            returnVar.setRuntimeTypeName(typeInfoSymbol.getRuntimeTypeName());

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
