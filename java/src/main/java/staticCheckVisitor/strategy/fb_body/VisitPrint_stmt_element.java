package staticCheckVisitor.strategy.fb_body;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCScopeStack;
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
        String varName = ctx.identifier().getText();
        PLCSymbol found = currentScope.deepFindSymbol(varName);
        if (found instanceof PLCVariable varSymbol) {
            PLCVariable copy = new PLCVariable(varSymbol);
            // 使用变量名作为 assignVar，translateExpr 的 step 7 会将其转为 gvl.read<TYPE>(offset)
            copy.setAssignVar(varName);
            ArrayList<PLCSymbol> result = new ArrayList<>();
            result.add(copy);
            return result;
        }
        throw new PLCSemanticException("PRINT: cannot find variable " + varName);
    }
}
