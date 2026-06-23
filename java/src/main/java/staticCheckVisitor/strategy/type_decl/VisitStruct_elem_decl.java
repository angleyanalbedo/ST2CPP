package staticCheckVisitor.strategy.type_decl;

import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_struct_elem_decl;

@StrategyForVisit(ruleIndex = RULE_struct_elem_decl)
public class VisitStruct_elem_decl implements Strategy{
    /**
     * 返回拥有loaction 以及_spec_init信息的PLCVariable
     * */
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Struct_elem_declContext ctx = (PLCSTPARSERParser.Struct_elem_declContext) parserCtx;
        //struct_elem_name
        String name = ctx.struct_elem_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);
        PLCVariable structElement = new PLCVariable();
        structElement.setName(name);

        //(( located_at multibit_part_access ? )? ':'| located_at_init':' )
        if(ctx.located_at_init() != null){
            structElement.setLocation(ctx.located_at_init().getText());
        }else if(ctx.located_at() != null){
            StringBuilder location = new StringBuilder(ctx.located_at().getText());
            if(ctx.multibit_part_access() != null){
                location.append(ctx.multibit_part_access().getText());
            }
            structElement.setLocation(new String(location));
        }
        //访问最后一个节点获得PLCVariable
        PLCVariable elemSpecInit = (PLCVariable) visitor.visit(ctx.getChild(ctx.getChildCount() - 1)).get(0);

        structElement.setSort(elemSpecInit.getSort());
        structElement.setTypeId(elemSpecInit.getTypeId());
        structElement.setAssignVar(elemSpecInit.getAssignVar());
        structElement.setRuntimeTypeName(elemSpecInit.getRuntimeTypeName());
        structElement.setDeclSymbol(elemSpecInit.getDeclSymbol());


        return visitor.packSymbols(structElement);
    }
}
