package staticCheckVisitor.strategy.var_decls.func_var_decls;

import PLCSymbolAndScope.PLCSymbols.PLCModifierEnum;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.VisitorTool;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;
import java.util.Objects;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_elem_type_name;
import static antlr4.PLCSTPARSERParser.RULE_var_decls;

@StrategyForVisit(ruleIndex = RULE_var_decls)
public class VisitVar_decls  implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Var_declsContext ctx = (PLCSTPARSERParser.Var_declsContext) parserCtx;
        //创建临时符号，用于保存当前层的信息,用于最后的组装
        PLCVariable tempVariable = new PLCVariable();
        //CONST信息
        tempVariable.ifConst = Objects.equals(ctx.getChild(1).getText(), "CONSTANT");
        //变量段类型信息
        tempVariable.varSections = PLCModifierEnum.VarSections.VAR;

        //获取变量段声明成员信息
        if(ctx.Access_Spec() != null){
            tempVariable.accessModifier = visitor.visitorTool.getAccessType(ctx.Access_Spec().getText());
        }

        //目标符号的列表
        ArrayList<PLCSymbol> targetList = new ArrayList<>();

        //访问变量段拿出下层返回的符号变量
        for (PLCSTPARSERParser.Var_decl_initContext var_decl_initContext : ctx.var_decl_init()) {
            targetList.addAll(visitor.visit(var_decl_initContext));
        }

        //在下层加入符号表，在此层添加信息
        for (PLCSymbol var : targetList) {
            visitor.visitorTool.settleVarAttrs(tempVariable, (PLCVariable) var);
        }
        return targetList;
    }
}
