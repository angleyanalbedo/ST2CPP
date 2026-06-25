package staticCheckVisitor.strategy.fb_body;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_print_stmt_element;

@StrategyForVisit(ruleIndex = RULE_print_stmt_element)
public class VisitPrint_stmt_element implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Print_stmt_elementContext ctx = (PLCSTPARSERParser.Print_stmt_elementContext) parserCtx;
        ArrayList<PLCSymbol> result = new ArrayList<>();

        // 字符串字面量：PRINT("hello")
        if (ctx.D_byte_char() != null) {
            String raw = ctx.D_byte_char().getText();
            // 去掉两端引号
            String content = raw.substring(1, raw.length() - 1);
            PLCVariable strVar = new PLCVariable();
            strVar.setSort(PLCModifierEnum.Sort.STRING);
            strVar.setAssignVar("\"" + content + "\"");
            result.add(strVar);
            return result;
        }
        if (ctx.s_byte_char() != null) {
            String raw = ctx.s_byte_char().getText();
            String content = raw.substring(1, raw.length() - 1);
            PLCVariable strVar = new PLCVariable();
            strVar.setSort(PLCModifierEnum.Sort.STRING);
            strVar.setAssignVar("\"" + content + "\"");
            result.add(strVar);
            return result;
        }

        // 标识符：PRINT(A)
        String varName = ctx.identifier().getText();
        PLCSymbol found = currentScope.deepFindSymbol(varName);
        if (found instanceof PLCVariable varSymbol) {
            PLCVariable copy = new PLCVariable(varSymbol);
            copy.setAssignVar(varName);
            result.add(copy);
            return result;
        }
        throw new PLCSemanticException("PRINT: cannot find variable " + varName);
    }
}
