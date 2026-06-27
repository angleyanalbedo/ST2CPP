package staticCheckVisitor.strategy.var_decls.var_spec_init;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCArrayDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_array_spec_init;

@StrategyForVisit(ruleIndex = RULE_array_spec_init)
public class VisitArray_spec_init implements Strategy {
    /**
     * 返回拥有初始值、typeid、sort RuntimeTypeName的PLCVariable
     * 同时解析子范围信息存到 arrayBounds
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Array_spec_initContext ctx = (PLCSTPARSERParser.Array_spec_initContext) parserCtx;
        //收集下层信息，获取类型信息
        PLCTypeDeclSymbol arrayType = (PLCTypeDeclSymbol) visitor.visit(ctx.array_spec()).get(0);
        PLCArrayDeclSymbol basicArrayType = (PLCArrayDeclSymbol) PLCTotalSymbolTable.getTypeByTypeID(arrayType.getTypeId());

        //要返回的符号
        PLCVariable arrayInitVar = new PLCVariable();
        arrayInitVar.setTypeId(basicArrayType.getTypeId());
        arrayInitVar.setSort(basicArrayType.getVarSort());
        arrayInitVar.setAssignVar(basicArrayType.getInitVar());

        // 解析子范围信息，存到 arrayBounds
        PLCSTPARSERParser.Array_specContext arraySpec = ctx.array_spec();
        if (arraySpec != null) {
            int dimCount = arraySpec.subrange().size();
            int[][] bounds = new int[dimCount][];
            for (int i = 0; i < dimCount; i++) {
                PLCSTPARSERParser.SubrangeContext subrange = arraySpec.subrange(i);
                // subrange 包含两个 constant_expr
                PLCVariable lowerVar = (PLCVariable) visitor.visit(subrange.constant_expr(0)).get(0);
                PLCVariable upperVar = (PLCVariable) visitor.visit(subrange.constant_expr(1)).get(0);
                int lower = parseBound(lowerVar.getAssignVar());
                int upper = parseBound(upperVar.getAssignVar());
                int count = upper - lower + 1;
                bounds[i] = new int[]{lower, upper, count};
            }
            arrayInitVar.setArrayBounds(bounds);
        }

        // 构造数组类型的 runtimeTypeName，格式：ARRAY[low..high] OF elemType
        StringBuilder arrayTypeName = new StringBuilder("ARRAY[");
        if (arraySpec != null) {
            for (int i = 0; i < arraySpec.subrange().size(); i++) {
                PLCSTPARSERParser.SubrangeContext subrange = arraySpec.subrange(i);
                if (i > 0) arrayTypeName.append(",");
                arrayTypeName.append(subrange.getText());
            }
            arrayTypeName.append("] OF ");
            PLCSymbol elemTypeSymbol = visitor.visit(arraySpec.data_type_access()).get(0);
            arrayTypeName.append(elemTypeSymbol.getName());
        }
        arrayInitVar.setRuntimeTypeName(arrayTypeName.toString());

        //访问下层
        if(ctx.array_init() != null){
            PLCVariable arrayInit = (PLCVariable) visitor.visit(ctx.array_init()).get(0);
            arrayInitVar.setAssignVar(arrayInit.getAssignVar());
        }
        return visitor.packSymbols(arrayInitVar);
    }

    private int parseBound(String boundExpr) {
        if (boundExpr == null) return 0;
        String s = boundExpr.trim();
        if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length() - 1).trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
