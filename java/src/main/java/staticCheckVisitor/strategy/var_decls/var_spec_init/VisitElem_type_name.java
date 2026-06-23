package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.basicTypeTable;
import static antlr4.PLCSTPARSERParser.RULE_elem_type_name;

@StrategyForVisit(ruleIndex = RULE_elem_type_name)
public class VisitElem_type_name implements Strategy {
    /**
     * 返回 类型信息 默认初始值 runtimename，返回类型为PLCTypeDeclSymbol
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Elem_type_nameContext ctx = (PLCSTPARSERParser.Elem_type_nameContext) parserCtx;
        //获取变量类型名称
        String typeName = ctx.getText();
        PLCTypeDeclSymbol basicType = (PLCTypeDeclSymbol) basicTypeTable.findSymbol(typeName);
        PLCTypeDeclSymbol targetVar = new PLCTypeDeclSymbol(basicType);
        targetVar.setRuntimeTypeName(basicType.getRuntimeName());

        //打包返回
        return visitor.packSymbols(targetVar);

    }
}