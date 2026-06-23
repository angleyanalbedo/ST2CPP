package staticCheckVisitor.strategy.variable_access;

import PLCException.PLCSemanticException;
import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import java.util.Objects;

import static PLCSymbolAndScope.PLCScopeStack.currentScope;
import static antlr4.PLCSTPARSERParser.RULE_symbolic_variable;

@StrategyForVisit(ruleIndex = RULE_symbolic_variable,branch = 0)
public class VisitThis_symbol implements Strategy {
    /**
     * 暂时只支持普通变量在类内的索引，引用、结构体以及数组暂未实现
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.ThisSymbolicContext ctx = (PLCSTPARSERParser.ThisSymbolicContext) parserCtx;
        //获取变量名称
        String varName = ctx.identifier().getText();
        PLCSymbol tempFoundSymbol;
        if (ctx.getChild(0).getText().equals("THIS")) {//THIS.A, 此时在当前作用域的符号表内搜索
            tempFoundSymbol = PLCScopeStack.currentScope.getParentScope().shallowFindSymbol(varName);
        }
        else{ //A, 此时在当前作用域内进行深搜索
            //先检查是不是方法的返回值
            PLCImportScopeTypeDeclType declSymbol = currentScope.getDeclSymbol();
            //变量为方法的直接返回值
            if(declSymbol instanceof PLCBaseFUNDeclSymbol funSymbol &&
                    funSymbol.getReturnTypeId() != -1 &&
                    varName.equals(declSymbol.getName())){
                int returnTypeId = funSymbol.getReturnTypeId();
                funSymbol.setIfReturned(true);
                tempFoundSymbol = new PLCVariable();
                tempFoundSymbol.setTypeId(returnTypeId);
                tempFoundSymbol.setName("*this->returnValue ");

            }else{
                PLCSymbol symbolItself = currentScope.deepFindSymbol(varName);
                if(symbolItself instanceof PLCVariable castedSymbol){
                    tempFoundSymbol = new PLCVariable(castedSymbol);
                    tempFoundSymbol.setName(symbolItself.getLocalScope() == currentScope ? "*"+varName : castedSymbol.getUniqueName());
                }else{
                    throw new PLCSemanticException("can not find variable named " + varName + " FROM : " + ctx.getText());
                }
            }
        }

        // '^'* ((array_index*)//ArrayList|(struct_variable*)//. var_access)的访问未完成
        return visitor.packSymbols(tempFoundSymbol);
    }
}
