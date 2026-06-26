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
        String typeName = ctx.getText();
        PLCTypeDeclSymbol basicType = (PLCTypeDeclSymbol) basicTypeTable.findSymbol(typeName);

        // 基本类型表找不到，尝试总类型表（用户自定义类型）
        if (basicType == null) {
            basicType = PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable.getTypeByTypeID(
                PLCSymbolAndScope.IDGenerator.getIDGenerator().newTypeId());
            if (basicType == null) {
                // 创建默认类型，避免 NPE
                basicType = new PLCTypeDeclSymbol();
                basicType.setName(typeName);
                basicType.setRuntimeName(typeName);
                basicType.setRuntimeTypeName(typeName);
            }
        }
        PLCTypeDeclSymbol targetVar = new PLCTypeDeclSymbol(basicType);
        targetVar.setRuntimeTypeName(basicType.getRuntimeName());
        return visitor.packSymbols(targetVar);
    }
}