package staticCheckVisitor;

import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.*;
import antlr4.PLCSTPARSERBaseVisitor;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import staticCheckVisitor.factory.Factory;
import staticCheckVisitor.strategy.var_decls.var_elem.VisitSubscript_list;
import staticCheckVisitor.strategy.var_decls.var_spec_init.VisitEnum_spec_init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PLCVisitor extends PLCSTPARSERBaseVisitor<ArrayList<PLCSymbol>> {
    public final VisitorTool visitorTool;
    public final ParseTreeProperty<ArrayList<PLCSymbol>> properties;
    private final Factory factory;
    public PLCVisitor(ParseTreeProperty<ArrayList<PLCSymbol>> properties){
        super();
        PLCScopeStack.stackInit();
        new GenerateBasicTypes().generate();
        visitorTool = VisitorTool.getTool();
        this.properties = properties;
        this.factory = Factory.getFactory();
    }

    //-----------------------工具方法-------------------------------------


    /**
     * 返回一串拥有 名称 信息的符号
     * 用于namespace(.namespace)*,获得namespace list
     * */
    public ArrayList<PLCSymbol> getNameSpaceList(List<PLCSTPARSERParser.Namespace_nameContext> nameList){
        ArrayList<PLCSymbol> nameSpaceList = new ArrayList<>();
        //获得命名空间列表nameSpaceList
        for(PLCSTPARSERParser.Namespace_nameContext nameContext : nameList){
            PLCNamespaceDeclSymbol namespaceDeclSymbol = new PLCNamespaceDeclSymbol();
            namespaceDeclSymbol.setName(nameContext.getText());
            nameSpaceList.add(namespaceDeclSymbol);
        }
        return nameSpaceList;
    }

    //将类型打包返回
    public ArrayList<PLCSymbol> packSymbols(PLCSymbol... symbols){
        return new ArrayList<>(Arrays.asList(symbols));
    }



    /*-----------------------------prog--------------------------------------------*/

    @Override
    public ArrayList<PLCSymbol> visitProg_decl(PLCSTPARSERParser.Prog_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        properties.put(ctx, symbols);
        return symbols;
    }

    /*------------------io var decls--------------------------------------------*/
    @Override
    public ArrayList<PLCSymbol> visitIo_var_decls(PLCSTPARSERParser.Io_var_declsContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitInput_decls(PLCSTPARSERParser.Input_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitInput_decl(PLCSTPARSERParser.Input_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }


    @Override
    public ArrayList<PLCSymbol> visitOutput_decls(PLCSTPARSERParser.Output_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitOutput_decl(PLCSTPARSERParser.Output_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }



    @Override
    public ArrayList<PLCSymbol> visitIn_out_decls(PLCSTPARSERParser.In_out_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitIn_out_var_decl(PLCSTPARSERParser.In_out_var_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /*---------------------func var decls---------------------------------------------*/

    @Override
    public ArrayList<PLCSymbol> visitFunc_var_decls(PLCSTPARSERParser.Func_var_declsContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    //external变量段内声明的变量需要添加到全局（GLOBAL）
    @Override
    public ArrayList<PLCSymbol> visitExternal_var_decls(PLCSTPARSERParser.External_var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitExternal_decl(PLCSTPARSERParser.External_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitGlobal_var_name(PLCSTPARSERParser.Global_var_nameContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * @deprecated 先收集本层能获得的信息，存入符号tempVariable，再从下层收集信息，最后组装，加入符号表。
     * */
    @Override
    public ArrayList<PLCSymbol> visitVar_decls(PLCSTPARSERParser.Var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * @deprecated 返回拥有 变量类型/分类与 名称 的符号数组
     * */
    @Override
    public ArrayList<PLCSymbol> visitVardeclinit(PLCSTPARSERParser.VardeclinitContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * @deprecated 收集名称信息
     * */
    @Override
    public ArrayList<PLCSymbol> visitVariable_list(PLCSTPARSERParser.Variable_listContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    //返回只拥有名称的一个符号
    @Override
    public ArrayList<PLCSymbol> visitVariable_name(PLCSTPARSERParser.Variable_nameContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 字符串规范初始化(含初始化信息)：访问收集 类型信息 ，进行初步检查，返回给上层
     * */
    @Override
    public ArrayList<PLCSymbol> visitStr_spec_init(PLCSTPARSERParser.Str_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * @deprecated 字符串规范（不含初始化信息）：收集“类型信息”，返回只含有typeid的符号给上层
     * */
    @Override
    public ArrayList<PLCSymbol> visitStr_spec(PLCSTPARSERParser.Str_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    //返回只有类型id的一个符号
    /**
     * @deprecated 简单基本类型规范初始化
     * 返回拥有类型类型信息（TYPEID SORT）的变量给上层
     * */
    @Override
    public ArrayList<PLCSymbol> visitSimple_spec_init(PLCSTPARSERParser.Simple_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }


    @Override
    public ArrayList<PLCSymbol> visitSimple_spec(PLCSTPARSERParser.Simple_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    //获取符号类型（获取基本类型）
    @Override
    public ArrayList<PLCSymbol> visitElem_type_name(PLCSTPARSERParser.Elem_type_nameContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    //类似class_access
    @Override
    public ArrayList<PLCSymbol> visitSimple_type_access(PLCSTPARSERParser.Simple_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /*--------------------------------------visitTemp_var_decls-----------------------------*/
    @Override
    public ArrayList<PLCSymbol> visitTemp_var_decls(PLCSTPARSERParser.Temp_var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitVar_decl(PLCSTPARSERParser.Var_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /*----------------------visitOther_var_decls—---------------------------------------*/

    @Override
    public ArrayList<PLCSymbol> visitOther_var_decls(PLCSTPARSERParser.Other_var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRetain_var_decls(PLCSTPARSERParser.Retain_var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitNo_retain_var_decls(PLCSTPARSERParser.No_retain_var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /*----------------------------------visitLoc_var_decls------------------------------------------*/
    @Override
    public ArrayList<PLCSymbol> visitLoc_var_decls(PLCSTPARSERParser.Loc_var_declsContext ctx) {
        return super.visitLoc_var_decls(ctx);
    }

    @Override
    public ArrayList<PLCSymbol> visitProg_access_decls(PLCSTPARSERParser.Prog_access_declsContext ctx) {
        return super.visitProg_access_decls(ctx);
    }

    //---------------------------Constant_expr------------------------------------------------

    /**
     * 在内部进行类型检查，如果在此处没有错误，就把最终的类型信息返回给上层
     * */
    @Override
    public ArrayList<PLCSymbol> visitConstant_expr(PLCSTPARSERParser.Constant_exprContext ctx) {
        ArrayList<PLCSymbol> plcSymbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, plcSymbols);
        return plcSymbols;
    }

    /**
     * 返回只具有类型id的符号
     * 分为两种情况处理：
     *  如果无比较动作，直接返回访问下层返回值
     *  如果有比较动作，检查类型 + 返回bool类型id符号
     * */

    @Override
    public ArrayList<PLCSymbol> visitExpression(PLCSTPARSERParser.ExpressionContext ctx) {
        ArrayList<PLCSymbol> plcSymbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, plcSymbols);
        return plcSymbols;
    }

    /**
     * 处理同 visitExpression
     * */
    @Override
    public ArrayList<PLCSymbol> visitXor_expr(PLCSTPARSERParser.Xor_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 处理同 visitExpression
     * */
    @Override
    public ArrayList<PLCSymbol> visitAnd_expr(PLCSTPARSERParser.And_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 处理类似 visitExpression
     * */
    @Override
    public ArrayList<PLCSymbol> visitCompare_expr(PLCSTPARSERParser.Compare_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 处理同 visitCompare_expr
     * */
    @Override
    public ArrayList<PLCSymbol> visitEqu_expr(PLCSTPARSERParser.Equ_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 访问下层每一个节点，获取类型，比较是否可以计算(checkListIfCompatible)
     * */
    @Override
    public ArrayList<PLCSymbol> visitAdd_expr(PLCSTPARSERParser.Add_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 同 visitAdd_expr
     * */
    @Override
    public ArrayList<PLCSymbol> visitTerm(PLCSTPARSERParser.TermContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 同 visitAdd_expr(兼容检查有区别)
     * */
    @Override
    public ArrayList<PLCSymbol> visitPower_expr(PLCSTPARSERParser.Power_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 三种情况：
     * 1. NOT ---primary必须为bool
     * 2. + / -  -----primary必须为calc
     * 3. 无 ------primary不规定
     * */
    @Override
    public ArrayList<PLCSymbol> visitUnary_expr(PLCSTPARSERParser.Unary_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 访问返回下层节点
     * */
    @Override
    public ArrayList<PLCSymbol> visitPrimary_expr(PLCSTPARSERParser.Primary_exprContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }


    /**
     * 返回子节点返回值
     * */
    @Override
    public ArrayList<PLCSymbol> visitConstant(PLCSTPARSERParser.ConstantContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 返回子节点返回值
     * */
    @Override
    public ArrayList<PLCSymbol> visitNumeric_literal(PLCSTPARSERParser.Numeric_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 返回类型id 为INT的符号
     * */
    @Override
    public ArrayList<PLCSymbol> visitInt_literal(PLCSTPARSERParser.Int_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * 返回类型id 为REAL的符号
     * */
    @Override
    public ArrayList<PLCSymbol> visitReal_literal(PLCSTPARSERParser.Real_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }


    @Override
    public ArrayList<PLCSymbol> visitChar_literal(PLCSTPARSERParser.Char_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitTime_literal(PLCSTPARSERParser.Time_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitBit_str_literal(PLCSTPARSERParser.Bit_str_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitBool_literal(PLCSTPARSERParser.Bool_literalContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /**
     * visit enum_value : 返回变量的类型id
     * 分两种情况：
     * 1. enum枚举常量
     * 2. 普通变量常量
     * */
    @Override
    public ArrayList<PLCSymbol> visitEnum_value(PLCSTPARSERParser.Enum_valueContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    //----------------------------------------fb_body---------------------------------------------

    @Override
    public ArrayList<PLCSymbol> visitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitStmt_list(PLCSTPARSERParser.Stmt_listContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitStmt(PLCSTPARSERParser.StmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitVariableAssignExpression(PLCSTPARSERParser.VariableAssignExpressionContext ctx){
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        properties.put(ctx, symbols);
        return symbols;
    }

    /*------------------------------if/switch------------------------------------------------*/

    @Override
    public ArrayList<PLCSymbol> visitSelection_stmt(PLCSTPARSERParser.Selection_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitIf_stmt(PLCSTPARSERParser.If_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitElsif_stmt(PLCSTPARSERParser.Elsif_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitElse_stmt(PLCSTPARSERParser.Else_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitCase_stmt(PLCSTPARSERParser.Case_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitCase_selection(PLCSTPARSERParser.Case_selectionContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitCase_list(PLCSTPARSERParser.Case_listContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitCase_list_elem(PLCSTPARSERParser.Case_list_elemContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitSubrange(PLCSTPARSERParser.SubrangeContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /*-------------------------for/while-------------------------------------------*/
    @Override
    public ArrayList<PLCSymbol> visitIteration_stmt(PLCSTPARSERParser.Iteration_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitFor_stmt(PLCSTPARSERParser.For_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitControl_variable(PLCSTPARSERParser.Control_variableContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitBy_list(PLCSTPARSERParser.By_listContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitWhile_stmt(PLCSTPARSERParser.While_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRepeat_stmt(PLCSTPARSERParser.Repeat_stmtContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    /*----------------------------func_decl-------------------------------------------------*/

    @Override
    public ArrayList<PLCSymbol> visitFunc_decl(PLCSTPARSERParser.Func_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx,symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitData_type_access(PLCSTPARSERParser.Data_type_accessContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitUsing_directive(PLCSTPARSERParser.Using_directiveContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitNamespace_h_name(PLCSTPARSERParser.Namespace_h_nameContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitFunc_body(PLCSTPARSERParser.Func_bodyContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitClass_decl(PLCSTPARSERParser.Class_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitClass_type_access(PLCSTPARSERParser.Class_type_accessContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitInterface_name_list(PLCSTPARSERParser.Interface_name_listContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitInterface_type_access(PLCSTPARSERParser.Interface_type_accessContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitMethod_decl(PLCSTPARSERParser.Method_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitInterface_decl(PLCSTPARSERParser.Interface_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitMethod_prototype(PLCSTPARSERParser.Method_prototypeContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitGlobal_var_decls(PLCSTPARSERParser.Global_var_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }


    @Override
    public ArrayList<PLCSymbol> visitGlobal_var_decl(PLCSTPARSERParser.Global_var_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitNamespace_decl(PLCSTPARSERParser.Namespace_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitNamespace_elements(PLCSTPARSERParser.Namespace_elementsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitFb_decl(PLCSTPARSERParser.Fb_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitFb_io_var_decls(PLCSTPARSERParser.Fb_io_var_declsContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitFb_input_decls(PLCSTPARSERParser.Fb_input_declsContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitDirectNum(PLCSTPARSERParser.DirectNumContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitUser_defination_spec_init(PLCSTPARSERParser.User_defination_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitUser_defination_type_access(PLCSTPARSERParser.User_defination_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitCallFunc(PLCSTPARSERParser.CallFuncContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex(), 0).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitFunc_call(PLCSTPARSERParser.Func_callContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex(), 0).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitFunc_access(PLCSTPARSERParser.Func_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitInputParam(PLCSTPARSERParser.InputParamContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex(), 0).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitOutParam(PLCSTPARSERParser.OutParamContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex(), 2).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_var_decl_init(PLCSTPARSERParser.Array_var_decl_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_spec_init(PLCSTPARSERParser.Array_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_spec(PLCSTPARSERParser.Array_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_init(PLCSTPARSERParser.Array_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_elem_init(PLCSTPARSERParser.Array_elem_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_elem_init_value(PLCSTPARSERParser.Array_elem_init_valueContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_elem_item_init(PLCSTPARSERParser.Array_elem_item_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitData_type_decl(PLCSTPARSERParser.Data_type_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitSubrange_type_decl(PLCSTPARSERParser.Subrange_type_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitSubrange_spec(PLCSTPARSERParser.Subrange_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitSubrange_spec_init(PLCSTPARSERParser.Subrange_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitEnum_type_access(PLCSTPARSERParser.Enum_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitEnum_spec_init(PLCSTPARSERParser.Enum_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitEnum_value_spec(PLCSTPARSERParser.Enum_value_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitNamed_spec_init(PLCSTPARSERParser.Named_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitStruct_elem_init(PLCSTPARSERParser.Struct_elem_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitStruct_init(PLCSTPARSERParser.Struct_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitStruct_spec(PLCSTPARSERParser.Struct_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitStruct_spec_init(PLCSTPARSERParser.Struct_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitStruct_decl(PLCSTPARSERParser.Struct_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }
    @Override
    public ArrayList<PLCSymbol> visitStruct_type_access(PLCSTPARSERParser.Struct_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitSubrange_type_access(PLCSTPARSERParser.Subrange_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitStr_type_decl(PLCSTPARSERParser.Str_type_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitGlobal_var_spec(PLCSTPARSERParser.Global_var_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitLoc_var_spec_init(PLCSTPARSERParser.Loc_var_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitFb_type_access(PLCSTPARSERParser.Fb_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitSimple_type_decl(PLCSTPARSERParser.Simple_type_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }
    @Override
    public ArrayList<PLCSymbol> visitType_decl(PLCSTPARSERParser.Type_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_type_decl(PLCSTPARSERParser.Array_type_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_conform_decl(PLCSTPARSERParser.Array_conform_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_type_access(PLCSTPARSERParser.Array_type_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitArray_var_decl(PLCSTPARSERParser.Array_var_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        this.properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitLoc_partly_var_decl(PLCSTPARSERParser.Loc_partly_var_declContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitLoc_partly_var(PLCSTPARSERParser.Loc_partly_varContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitVar_spec(PLCSTPARSERParser.Var_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitScope_name(PLCSTPARSERParser.Scope_nameContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRef_spec_init(PLCSTPARSERParser.Ref_spec_initContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRef_spec(PLCSTPARSERParser.Ref_specContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRef_value(PLCSTPARSERParser.Ref_valueContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRef_addr(PLCSTPARSERParser.Ref_addrContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }


    @Override
    public ArrayList<PLCSymbol> visitVariable_access(PLCSTPARSERParser.Variable_accessContext ctx) {
        ArrayList<PLCSymbol> symbols = factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
        properties.put(ctx, symbols);
        return symbols;
    }

    @Override
    public ArrayList<PLCSymbol> visitVariable(PLCSTPARSERParser.VariableContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitThisSymbolic(PLCSTPARSERParser.ThisSymbolicContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitStruct_elem_select(PLCSTPARSERParser.Struct_elem_selectContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitStruct_variable(PLCSTPARSERParser.Struct_variableContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitSubscript(PLCSTPARSERParser.SubscriptContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitSubscript_list(PLCSTPARSERParser.Subscript_listContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitVar_access(PLCSTPARSERParser.Var_accessContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRef_assign(PLCSTPARSERParser.Ref_assignContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitRef_deref(PLCSTPARSERParser.Ref_derefContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

    @Override
    public ArrayList<PLCSymbol> visitNamespaceSymbolic(PLCSTPARSERParser.NamespaceSymbolicContext ctx) {
        return factory.getStrategy(ctx.getRuleIndex()).invoke(ctx, this);
    }

}
