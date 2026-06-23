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

import static antlr4.PLCSTPARSERParser.RULE_str_spec_init;

@StrategyForVisit(ruleIndex = RULE_str_spec_init)
public class VisitStr_spec_init implements Strategy {
    /**
     * 返回拥有初始值、typeid、sort RuntimeTypeName的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Str_spec_initContext ctx = (PLCSTPARSERParser.Str_spec_initContext) parserCtx;
        //收集下层信息
        PLCTypeDeclSymbol strType = (PLCTypeDeclSymbol) visitor.visit(ctx.str_spec()).get(0);

        //要返回的符号
        PLCVariable strInitVar = new PLCVariable();
        strInitVar.setTypeId(strType.getTypeId());
        strInitVar.setSort(strType.getVarSort());
        strInitVar.setAssignVar(strType.getInitVar());
        strInitVar.setRuntimeTypeName(strType.getRuntimeTypeName());

        //访问下层,进行类型检查
        if(ctx.constant_expr() != null){
            PLCVariable constType = (PLCVariable) visitor.visit(ctx.constant_expr()).get(0);
            try{
                PLCTypeDeclSymbol symbolType = PLCTotalSymbolTable.getTypeByTypeID(strType.getTypeId());
                if(!symbolType.checkCanAssignWith(constType.getTypeId())){
                    throw new PLCSemanticException("type mismatch : " + ctx.getText());
                }
                //填入初始值
                strInitVar.setAssignVar(constType.getAssignVar());
            }catch(PLCSemanticException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }

        //将下层收集的信息返回给上层
        return visitor.packSymbols(strInitVar);
    }
}
