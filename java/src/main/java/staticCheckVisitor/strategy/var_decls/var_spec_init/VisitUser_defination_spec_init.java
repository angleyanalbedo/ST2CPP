package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
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

        //如果存在constant_expr，则进行类型检查，并重新赋初始值
        if(ctx.constant_expr() != null){
            PLCVariable constExpr = (PLCVariable) visitor.visit(ctx.constant_expr()).get(0);
            PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(typeId);
            try{
                if(!symbolType.checkCanAssignWith(constExpr.getTypeId())){
                    throw new PLCSemanticException("type mismatch :" + ctx.getText());
                }
                //填入初始值
                userDefinationVar.setAssignVar(constExpr.getAssignVar());
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }

        return visitor.packSymbols(userDefinationVar);
    }
}
