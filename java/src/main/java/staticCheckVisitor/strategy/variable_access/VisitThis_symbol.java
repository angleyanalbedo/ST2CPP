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
     * 支持普通变量、数组索引访问
     * 结构体成员访问暂未实现
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

        // 处理数组索引访问：ARR[I] -> 元素类型
        if (!ctx.array_index().isEmpty()) {
            // 获取当前变量的类型 ID
            int currentTypeId = tempFoundSymbol.getTypeId();
            PLCTypeDeclSymbol typeSymbol = PLCTotalSymbolTable.getTypeByTypeID(currentTypeId);

            // 遍历每个 array_index（多维数组支持）
            for (int i = 0; i < ctx.array_index().size(); i++) {
                if (typeSymbol instanceof PLCArrayDeclSymbol arrayType) {
                    // 更新类型为数组元素类型
                    currentTypeId = arrayType.getElementTypeId();
                    tempFoundSymbol.setTypeId(currentTypeId);
                    typeSymbol = PLCTotalSymbolTable.getTypeByTypeID(currentTypeId);

                    // 更新变量名，添加索引访问后缀（用于代码生成）
                    // array_index 的 getText() 包含方括号，如 "[I]"，需要去掉
                    String indexExpr = ctx.array_index(i).getText();
                    if (indexExpr.startsWith("[") && indexExpr.endsWith("]")) {
                        indexExpr = indexExpr.substring(1, indexExpr.length() - 1);
                    }
                    String currentName = tempFoundSymbol.getName();
                    // 移除前导的 *（如果有）
                    if (currentName.startsWith("*")) {
                        currentName = currentName.substring(1);
                    }
                    tempFoundSymbol.setName("*(" + currentName + "[" + indexExpr + "])");
                    if (tempFoundSymbol instanceof PLCVariable) {
                        PLCVariable tempVar = (PLCVariable) tempFoundSymbol;
                        // 使用变量名（currentName）构造 assignVar，不包含前导 *
                        // 因为数组元素访问作为右值时不需要解引用
                        tempVar.setAssignVar("(" + currentName + "[" + indexExpr + "])");
                    }
                } else {
                    throw new PLCSemanticException("type " + typeSymbol.getName() + " is not subscriptable: " + ctx.getText());
                }
            }
        }

        // struct_variable 访问暂未实现

        return visitor.packSymbols(tempFoundSymbol);
    }
}
