package staticCheckVisitor.strategy.pou_decl;

import PLCSymbolAndScope.IDGenerator;
import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.ParserRuleContext;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.StrategyForVisit;
import staticCheckVisitor.strategy.Strategy;

import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.currentSymbolTable;
import static antlr4.PLCSTPARSERParser.RULE_fb_decl;
import static antlr4.PLCSTPARSERParser.RULE_namespace_elements;

@StrategyForVisit(ruleIndex = RULE_fb_decl)
public class VisitFb_decl implements Strategy {
    @Override
    public ArrayList<PLCSymbol> invoke(ParserRuleContext parserCtx, PLCVisitor visitor) {
        PLCSTPARSERParser.Fb_declContext ctx = (PLCSTPARSERParser.Fb_declContext) parserCtx;

        //检查重名
        String name = ctx.derived_fb_name().getText();
        visitor.visitorTool.checkNameOnly(currentSymbolTable, name);

        //创建classSymbol
        PLCFBDeclSymbol plcfbDeclSymbol = new PLCFBDeclSymbol();
        plcfbDeclSymbol.setName(name);
        plcfbDeclSymbol.setRuntimeName(name);

        //分配符号id和类型id
        plcfbDeclSymbol.setSymbolId(IDGenerator.getIDGenerator().newSymbolId());
        plcfbDeclSymbol.setTypeId(IDGenerator.getIDGenerator().newTypeId());

        //加入符号表
        currentSymbolTable.addSymbol(plcfbDeclSymbol);

        //入栈
        PLCScopeStack.push(plcfbDeclSymbol);

        //总表
        PLCTotalSymbolTable.addType(plcfbDeclSymbol);
        PLCTotalSymbolTable.addBlock(plcfbDeclSymbol);

        //FINALORABSTRACT
        if(ctx.FINALORABSTRACT() != null){
            plcfbDeclSymbol.setClassModifier(ctx.FINALORABSTRACT().getText());
        }

        //设置方法使用的命名空间，命名空间的合法性在using_directive中检查
        for (PLCSTPARSERParser.Using_directiveContext using_directiveContext : ctx.using_directive()) {
            ArrayList<PLCSymbol> namespaceSymbols = visitor.visit(using_directiveContext);
            PLCScope importScope = plcfbDeclSymbol.getImportScope();
            for (PLCSymbol symbol : namespaceSymbols) {
                PLCNamespaceDeclSymbol namespaceSymbol = (PLCNamespaceDeclSymbol) symbol;
                importScope.addUsingNamespace(namespaceSymbol);
            }
        }


        //class_type_access
        //依照class_decl修改
        if(ctx.class_type_access() != null){
            PLCClassDeclSymbol extendClass = (PLCClassDeclSymbol)visitor.visit(ctx.class_type_access()).get(0);
            plcfbDeclSymbol.setBaseClass(extendClass);
        }
        //fb_type_access
        if(ctx.fb_type_access() != null){
            PLCFBDeclSymbol extendsFB = (PLCFBDeclSymbol) visitor.visit(ctx.fb_type_access()).get(0);
            plcfbDeclSymbol.setBaseClass(extendsFB);
        }


        //interface_name_list
        //依照class_decl修改
        if(ctx.interface_name_list() != null){
            ArrayList<PLCSymbol> interfaceList = visitor.visit(ctx.interface_name_list());
            for (PLCSymbol symbol : interfaceList) {
                PLCInterfaceDeclSymbol interfaceDeclSymbol = (PLCInterfaceDeclSymbol) symbol;
                plcfbDeclSymbol.addInterface(interfaceDeclSymbol);
            }
        }

        //fb_io_var_decls
        //依照class_decl修改
        for (PLCSTPARSERParser.Fb_io_var_declsContext fb_io_var_decl : ctx.fb_io_var_decls()) {
            visitor.visit(fb_io_var_decl);
        }

        //func_var_decls
        //依照class_decl修改
        for(PLCSTPARSERParser.Func_var_declsContext declsContext : ctx.func_var_decls()){
            visitor.visit(declsContext);
        }

        //temp_var_decl
        //依照class_decl修改
        for (PLCSTPARSERParser.Temp_var_declsContext temp_var_decl : ctx.temp_var_decls()) {
            visitor.visit(temp_var_decl);
        }

        //other_var_decls
        //依照class_decl修改
        for(PLCSTPARSERParser.Other_var_declsContext declsContext : ctx.other_var_decls()){
            visitor.visit(declsContext);
        }

        //method_decl
        for (PLCSTPARSERParser.Method_declContext methodDeclContext : ctx.method_decl()) {
            visitor.visit(methodDeclContext);
        }

        if(ctx.fb_body() != null){
            visitor.visit(ctx.fb_body());
        }

        //出栈
        PLCScopeStack.pop();
        return null;
    }
}