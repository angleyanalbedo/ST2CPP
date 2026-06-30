package staticCheckVisitor.strategy.constant_expr;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static antlr4.PLCSTPARSERParser.RULE_int_literal;

@StrategyForVisit(ruleIndex = RULE_int_literal)
public class VisitInt_literal implements Strategy {
    /**
     * 返回类型id 为INT的符号
     * 存在针对值的强制转型，runtime支持后需要修改
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Int_literalContext ctx = (PLCSTPARSERParser.Int_literalContext) parserCtx;
        //存在强制转型
        if(ctx.int_type_name() != null){
            //获得强制转换的类型
            PLCTypeDeclSymbol forceTranslateType = (PLCTypeDeclSymbol) PLCScopeStack.basicTypeTable.findSymbol(ctx.int_type_name().getText());

            PLCVariable intSymbol = new PLCVariable();
            intSymbol.setTypeId(forceTranslateType.getTypeId());
            intSymbol.setSort(PLCModifierEnum.Sort.INT);
            String constNum = ctx.getChild(ctx.getChildCount()-1).getText();
            constNum = convertSTNumeric(constNum);

            intSymbol.setAssignVar("(" + constNum + ")");

//            intSymbol.setAssignVar( "new" + forceTranslateType.getRuntimeName() + "(" + constNum + ")");
            return visitor.packSymbols(intSymbol);
        }else { //不存在强制转型
            PLCVariable intSymbol = new PLCVariable();
            intSymbol.setTypeId(IDGenerator.INTID);
            intSymbol.setSort(PLCModifierEnum.Sort.INT);
            String constNum = ctx.getChild(ctx.getChildCount()-1).getText();
            intSymbol.setAssignVar("(" + convertSTNumeric(constNum) + ")");
            return visitor.packSymbols(intSymbol);
        }

    }

    /** 将 ST 数值前缀转为 C++ 格式：16#→0x, 2#→0b, 8#→0 */
    public static String convertSTNumeric(String s) {
        if (s == null) return null;
        // typed literal like DINT#16#FF → strip type prefix first
        String result = s;
        if (result.matches("^[A-Za-z_]+#.*")) {
            int hash = result.indexOf('#');
            result = result.substring(hash + 1);
        }
        if (result.startsWith("16#")) {
            result = "0x" + result.substring(3);
        } else if (result.startsWith("2#")) {
            result = "0b" + result.substring(2);
        } else if (result.startsWith("8#")) {
            result = "0" + result.substring(2);
        }
        return result;
    }
}