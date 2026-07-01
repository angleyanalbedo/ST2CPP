package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCStructDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static antlr4.PLCSTPARSERParser.RULE_user_defination_spec_init;

@StrategyForVisit(ruleIndex = RULE_user_defination_spec_init)
public class VisitUser_defination_spec_init implements Strategy {
    /**
     * 返回拥有初始值、typeid、sort runtimeTypeName的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.User_defination_spec_initContext ctx = (PLCSTPARSERParser.User_defination_spec_initContext) parserCtx;

        //目标对象，最终将此符号打包返回
        PLCVariable userDefinationVar = new PLCVariable();

        //查询得到类型
        PLCTypeDeclSymbol userDefinationTypeSymbol = (PLCTypeDeclSymbol) visitor.visit(ctx.user_defination_type_access()).get(0);
        //组装信息
        int typeId = userDefinationTypeSymbol.getTypeId();
        userDefinationVar.setTypeId(typeId);
        userDefinationVar.setSort(userDefinationTypeSymbol.getVarSort());
        userDefinationVar.setAssignVar(userDefinationTypeSymbol.getInitVar());
        userDefinationVar.setRuntimeTypeName(userDefinationTypeSymbol.getRuntimeTypeName());

        //如果存在struct_init，则遍历命名参数，填充namedInit
        if(ctx.struct_init() != null){
            PLCVariable initVar = (PLCVariable) visitor.visit(ctx.struct_init()).get(0);
            PLCStructDeclSymbol initStruct = (PLCStructDeclSymbol) initVar.getDeclSymbol();
            if(initStruct != null){
                LinkedHashMap<String, String> namedInit = new LinkedHashMap<>();
                for(PLCVariable elem : initStruct.getVariables()){
                    namedInit.put(elem.getName(), elem.getAssignVar());
                }
                userDefinationVar.setAggregateInit(namedInit);
                // 保留 assignVar 作为字符串后备（兼容旧代码路径）
                userDefinationVar.setAssignVar(ctx.struct_init().getText());
            }
        }

        return visitor.packSymbols(userDefinationVar);
    }
}
