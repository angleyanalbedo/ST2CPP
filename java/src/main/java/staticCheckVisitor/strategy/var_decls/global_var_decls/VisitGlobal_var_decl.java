package staticCheckVisitor.strategy.var_decls.global_var_decls;

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

import static antlr4.PLCSTPARSERParser.RULE_global_var_decl;

@StrategyForVisit(ruleIndex = RULE_global_var_decl)
public class VisitGlobal_var_decl implements Strategy {
    /**
     * return  PLCVariable list
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Global_var_declContext ctx = (PLCSTPARSERParser.Global_var_declContext) parserCtx;
        //带有名称和位置信息的变量组合
        ArrayList<PLCSymbol> varSpec = visitor.visit(ctx.global_var_spec());
        //带有类型信息、初始值的变量
        PLCVariable varInfo = (PLCVariable) visitor.visit(ctx.getChild(2)).get(0);

        ArrayList<PLCSymbol> returnList = new ArrayList<>();
        for (PLCSymbol var : varSpec) {
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
