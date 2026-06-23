package staticCheckVisitor.strategy.pou_decl;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_method_decl;

@StrategyForVisit(ruleIndex = RULE_method_decl)
public class VisitMethod_decl implements Strategy {
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Method_declContext ctx = (PLCSTPARSERParser.Method_declContext) parserCtx;

        PLCMethodDeclSymbol methodDeclSymbol = new PLCMethodDeclSymbol();

        //method_name
        methodDeclSymbol.setName(ctx.method_name().getText());
        methodDeclSymbol.setRuntimeName("*" + methodDeclSymbol.getName() + "::callFunc");

        //分配符号id和类型id
        methodDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        methodDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(methodDeclSymbol);

        //入栈
        PLCScopeStack.push(methodDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(methodDeclSymbol);
        PLCTotalSymbolTable.addBlock(methodDeclSymbol);

        //Access_Spec
        if(ctx.Access_Spec() != null){
            methodDeclSymbol.setAccessModifier(ctx.Access_Spec().getText());
        }

        //FINALORABSTRACT
        if(ctx.FINALORABSTRACT() != null){
            methodDeclSymbol.setMethodModifier(ctx.FINALORABSTRACT().getText());
        }

        //OVERRIDE
        if(ctx.OVERRIDE() != null){
            methodDeclSymbol.setIfOverride(true);
        }

        //data_type_access
        //获得直接返回值类型和分类
        if(ctx.data_type_access() != null){
            PLCTypeDeclSymbol typedVar = (PLCTypeDeclSymbol) visitor.visit(ctx.data_type_access()).get(0);
            methodDeclSymbol.setReturnTypeId(typedVar.getTypeId());
            methodDeclSymbol.setReturnVarSort(typedVar.getVarSort());
        }

        //io_var_decls
        for (PLCSTPARSERParser.Io_var_declsContext io_var_decl : ctx.io_var_decls()) {
            ArrayList<PLCSymbol> symbols = visitor.visit(io_var_decl);
            for (PLCSymbol symbol : symbols) {
                PLCVariable var = (PLCVariable) symbol;
                methodDeclSymbol.addAccessVar(var);
                methodDeclSymbol.addVariable(var);
            }
        }

        //func_var_decls
        for (PLCSTPARSERParser.Func_var_declsContext funcVarDecl : ctx.func_var_decls()) {
            ArrayList<PLCSymbol> vars = visitor.visit(funcVarDecl);
            for (PLCSymbol varSymbol : vars) {
                PLCVariable var = (PLCVariable) varSymbol;
                if(var.getVarSections() != PLCModifierEnum.VarSections.VAR_GLOBAL){
                    methodDeclSymbol.addVariable(var);
                }
            }
        }

        //temp_var_decls
        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            ArrayList<PLCSymbol> vars = visitor.visit(temp_var_decl);
            for (PLCSymbol var : vars) {
                methodDeclSymbol.addVariable((PLCVariable) var);
            }
        }

        //func_body
        visitor.visit(ctx.func_body());

        //出栈
        PLCScopeStack.pop();

        //检查方法是不是有重复
        HashMap<Integer, PLCSymbol> symbolIDHashMap = currentSymbolTable.getSymbolIDHashMap();
        Iterator<Map.Entry<Integer, PLCSymbol>> iterator = symbolIDHashMap.entrySet().iterator();

        String methodName = methodDeclSymbol.getName();
        while(iterator.hasNext()){
            Map.Entry<Integer, PLCSymbol> symbolEntry = iterator.next();
            PLCSymbol symbol = symbolEntry.getValue();
            //找到重名的方法
            if(symbol.getSort() == PLCModifierEnum.Sort.METHOD_DECL && methodName.equals(symbol.getName()) && symbol != methodDeclSymbol){
                PLCMethodDeclSymbol overlappedMethod = (PLCMethodDeclSymbol) symbol;
                if(methodDeclSymbol.checkOverlap(overlappedMethod)){
                    try {
                        throw new PLCSemanticException("same function signature : " + ctx.getText());
                    } catch (PLCSemanticException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return visitor.packSymbols(methodDeclSymbol);
    }
}