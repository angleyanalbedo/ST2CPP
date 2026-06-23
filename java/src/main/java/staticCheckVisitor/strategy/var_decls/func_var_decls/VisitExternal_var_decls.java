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

import static PLCSymbolAndScope.PLCScopeStack.globalSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_elem_type_name;
import static antlr4.PLCSTPARSERParser.RULE_external_var_decls;

@StrategyForVisit(ruleIndex = RULE_external_var_decls)
public class VisitExternal_var_decls implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.External_var_declsContext ctx = (PLCSTPARSERParser.External_var_declsContext) parserCtx;
        //创建临时符号，用于保存当前层的信息,用于最后的组装
        PLCVariable tempVariable = new PLCVariable();
        //CONST信息
        tempVariable.ifConst = Objects.equals(ctx.getChild(1).getText(), "CONSTANT");
        //变量段类型信息
        tempVariable.varSections = PLCModifierEnum.VarSections.VAR_EXTERNAL;

        //目标符号的列表
        ArrayList<PLCSymbol> targetList = new ArrayList<>();

        //访问变量段拿出下层返回的符号变量
        for(PLCSTPARSERParser.External_declContext declContext: ctx.external_decl()){
            targetList.addAll(visitor.visit(declContext));
        }

        //先进行名称检查，再将变量符号与本层临时符号tempVariable组装并加入符号表
        for (PLCSymbol var : targetList) {
            visitor.visitorTool.settleVarAttrs(tempVariable, (PLCVariable) var);
        }
        return null;
    }
}
