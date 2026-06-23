package staticCheckVisitor.strategy.var_decls.temp_var_decls;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_conform_decl;

@StrategyForVisit(ruleIndex = RULE_array_conform_decl)
public class VisitArray_conform_decl implements Strategy{
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_conform_declContext ctx = (PLCSTPARSERParser.Array_conform_declContext) parserCtx;

        //访问variable_list获得只有变量名称的变量列表
        ArrayList<PLCSymbol> variableList = visitor.visit(ctx.variable_list());

        //访问获得变量类型信息(类型id)
        PLCTypeDeclSymbol varInfo = (PLCTypeDeclSymbol) visitor.visit(ctx.array_conformand()).get(0);

        ArrayList<PLCSymbol> returnList = new ArrayList<>();
        for (PLCSymbol var : variableList) {
            PLCVariable namedVar = (PLCVariable) var;
            PLCVariable returnVar = new PLCVariable();

            //组装信息
            returnVar.setName(namedVar.getName());
            returnVar.setLocation(namedVar.getLocation());

            returnVar.setTypeId(varInfo.getTypeId());
            returnVar.setSort(varInfo.getSort());
            returnVar.setAssignVar(varInfo.getInitVar());
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
