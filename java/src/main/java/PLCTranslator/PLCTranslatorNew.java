package PLCTranslator;

import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCTranslator.TranslateType.Class_decl.TranslateClass_decl;
import PLCTranslator.TranslateType.Fb_body.TranslateFb_body;
import PLCTranslator.TranslateType.Func_decl.TranslateFunc_decl;
import PLCTranslator.TranslateType.Interface_decl.TranslateInterface_decl;
import PLCTranslator.TranslateType.Method_decl.TranslateMethod_decl;
import PLCTranslator.TranslateType.Method_prototype.TranslateMethod_prototype;
import PLCTranslator.TranslateType.Namespace_decl.TranslateNamespace_decl;
import PLCTranslator.TranslateType.Namespace_decl.TranslateNamespace_elements;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Array_type_decl.TranslateArray_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Derived_type_decl.TranslateDerived_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Enum_type_decl.TranslateEnum_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Simple_type_decl.TranslateSimple_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Str_type_decl.TranslateStr_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_elem_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_spec;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.TranslateData_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.TranslateType_decl;
import PLCTranslator.TranslateType.Pou_decl.TranslateGlobal_var_decls;
import PLCTranslator.TranslateType.Pou_decl.TranslatePou_decl;
import PLCTranslator.TranslateType.Prog_decl.TranslateProg_decl;
import PLCTranslator.TranslateType.Startpoint.TranslateStartpoint;
import PLCTranslator.TranslateType.Stmt.Assign_stmt.TranslateRefAssignExpression;
import PLCTranslator.TranslateType.Stmt.Assign_stmt.TranslateRef_assign;
import PLCTranslator.TranslateType.Stmt.Assign_stmt.TranslateVariableAssignExpression;
import PLCTranslator.TranslateType.Stmt.Iteration_stmt.For_stmt.TranslateFor_stmt;
import PLCTranslator.TranslateType.Stmt.Iteration_stmt.Repeat_stmt.TranslateRepeat_stmt;
import PLCTranslator.TranslateType.Stmt.Iteration_stmt.TranslateIteration_stmt;
import PLCTranslator.TranslateType.Stmt.Iteration_stmt.While_stmt.TranslateWhile_stmt;
import PLCTranslator.TranslateType.Stmt.Print_stmt.TranslatePrint_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt.TranslateCase_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt.TranslateElsif_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt.TranslateIf_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.TranslateElse_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.TranslateSeletion_stmt;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.CallFunc.TranslateFunc_call;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation.TranslateInvocation1;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation.TranslateInvocation2;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateCallFunc;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateInvocationCall;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateReturn;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateSuperCall;
import PLCTranslator.TranslateType.Stmt.TranslateStmt;
import PLCTranslator.TranslateType.Stmt_list.TranslateStmt_list;
import antlr4.PLCSTPARSERBaseVisitor;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import java.util.ArrayList;

public class PLCTranslatorNew extends PLCSTPARSERBaseVisitor<ArrayList<String>> {
    //树节点信息
    static public ParseTreeProperty<ArrayList<PLCSymbol>> properties = new ParseTreeProperty<>();

    public PLCTranslatorNew(ParseTreeProperty<ArrayList<PLCSymbol>> properties) {
        PLCTranslatorNew.properties = properties;
    }

    /**
     * 翻译程序初始处
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitStartpoint(PLCSTPARSERParser.StartpointContext ctx) {
        TranslateStartpoint translateStartpoint = new TranslateStartpoint();
        translateStartpoint.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译main函数
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitProg_decl(PLCSTPARSERParser.Prog_declContext ctx) {
        TranslateProg_decl visitProg = new TranslateProg_decl();
        visitProg.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译程序块
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitPou_decl(PLCSTPARSERParser.Pou_declContext ctx) {
        TranslatePou_decl translatePou_decl = new TranslatePou_decl();
        translatePou_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译全局变量
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitGlobal_var_decls(PLCSTPARSERParser.Global_var_declsContext ctx) {
        TranslateGlobal_var_decls translateGlobal_var_decls = new TranslateGlobal_var_decls();
        translateGlobal_var_decls.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译函数
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitStmt_list(PLCSTPARSERParser.Stmt_listContext ctx) {
        TranslateStmt_list translateStmt_list = new TranslateStmt_list();
        translateStmt_list.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitStmt(PLCSTPARSERParser.StmtContext ctx) {
        TranslateStmt translateStmt = new TranslateStmt();
        translateStmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译引用赋值
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitRef_assign(PLCSTPARSERParser.Ref_assignContext ctx) {
        TranslateRef_assign translateRef_assign = new TranslateRef_assign();
        translateRef_assign.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译引用赋值
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitRefAssignExpression(PLCSTPARSERParser.RefAssignExpressionContext ctx) {
        TranslateRefAssignExpression translateRefAssignExpression = new TranslateRefAssignExpression();
        translateRefAssignExpression.translateNode(ctx, this);
        return null;
    }


    /**
     * 翻译变量赋值
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitVariableAssignExpression(PLCSTPARSERParser.VariableAssignExpressionContext ctx) {
        TranslateVariableAssignExpression translateVariableAssignExpression = new TranslateVariableAssignExpression();
        translateVariableAssignExpression.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译循环结构
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitIteration_stmt(PLCSTPARSERParser.Iteration_stmtContext ctx) {
        TranslateIteration_stmt translateIteration_stmt = new TranslateIteration_stmt();
        translateIteration_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译for循环结构
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitFor_stmt(PLCSTPARSERParser.For_stmtContext ctx) {
        TranslateFor_stmt translateFor_stmt = new TranslateFor_stmt();
        translateFor_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译repeat循环结构
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitRepeat_stmt(PLCSTPARSERParser.Repeat_stmtContext ctx) {
        TranslateRepeat_stmt translateRepeat_stmt = new TranslateRepeat_stmt();
        translateRepeat_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译while循环结构
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitWhile_stmt(PLCSTPARSERParser.While_stmtContext ctx) {
        TranslateWhile_stmt translateWhile_stmt = new TranslateWhile_stmt();
        translateWhile_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译print函数
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitPrint_stmt(PLCSTPARSERParser.Print_stmtContext ctx) {
        TranslatePrint_stmt translatePrint_stmt = new TranslatePrint_stmt();
        translatePrint_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译选择语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitSelection_stmt(PLCSTPARSERParser.Selection_stmtContext ctx) {
        TranslateSeletion_stmt translateSeletion_stmt = new TranslateSeletion_stmt();
        translateSeletion_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译else语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitElse_stmt(PLCSTPARSERParser.Else_stmtContext ctx) {
        TranslateElse_stmt translateElse_stmt = new TranslateElse_stmt();
        translateElse_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译else if语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitElsif_stmt(PLCSTPARSERParser.Elsif_stmtContext ctx) {
        TranslateElsif_stmt translateElsif_stmt = new TranslateElsif_stmt();
        translateElsif_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译if语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitIf_stmt(PLCSTPARSERParser.If_stmtContext ctx) {
        TranslateIf_stmt translateIf_stmt = new TranslateIf_stmt();
        translateIf_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译switch case语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitCase_stmt(PLCSTPARSERParser.Case_stmtContext ctx) {
        TranslateCase_stmt translateCase_stmt = new TranslateCase_stmt();
        translateCase_stmt.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译函数调用语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitCallFunc(PLCSTPARSERParser.CallFuncContext ctx) {
        TranslateCallFunc translateCallFunc = new TranslateCallFunc();
        translateCallFunc.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return null
     */
//    @Override
//    public ArrayList<String> visitInvocationCall(PLCSTPARSERParser.InvocationCallContext ctx) {
//        TranslateInvocationCall translateInvocationCall = new TranslateInvocationCall();
//        translateInvocationCall.translateNode(ctx, this);
//        return null;
//    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitInvocation1(PLCSTPARSERParser.Invocation1Context ctx) {
        TranslateInvocation1 translateInvocation1 = new TranslateInvocation1();
        translateInvocation1.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitInvocation2(PLCSTPARSERParser.Invocation2Context ctx) {
        TranslateInvocation2 translateInvocation2 = new TranslateInvocation2();
        translateInvocation2.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitReturn(PLCSTPARSERParser.ReturnContext ctx) {
        TranslateReturn translateReturn = new TranslateReturn();
        return null;
    }

    /**
     * 翻译类实例调用父类方法语句
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitSuperCall(PLCSTPARSERParser.SuperCallContext ctx) {
        TranslateSuperCall translateSuperCall = new TranslateSuperCall();
        translateSuperCall.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译命名空间声明
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitNamespace_decl(PLCSTPARSERParser.Namespace_declContext ctx) {
        TranslateNamespace_decl translateNamespace_decl = new TranslateNamespace_decl();
        translateNamespace_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译命名空间声明
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitNamespace_elements(PLCSTPARSERParser.Namespace_elementsContext ctx) {
        TranslateNamespace_elements translateNamespace_elements = new TranslateNamespace_elements();
        translateNamespace_elements.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译命名空间声明
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitMethod_prototype(PLCSTPARSERParser.Method_prototypeContext ctx) {
        TranslateMethod_prototype translateMethod_prototype = new TranslateMethod_prototype();
        translateMethod_prototype.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类内方法声明
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitMethod_decl(PLCSTPARSERParser.Method_declContext ctx) {
        TranslateMethod_decl translateMethod_decl = new TranslateMethod_decl();
        translateMethod_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译接口声明
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitInterface_decl(PLCSTPARSERParser.Interface_declContext ctx) {
        TranslateInterface_decl translateInterface_decl = new TranslateInterface_decl();
        translateInterface_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译main函数体
     * @param ctx the parse tree
     * @return null
     */
    @Override
    public ArrayList<String> visitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx) {
        TranslateFb_body translateFb_body = new TranslateFb_body();
        translateFb_body.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译函数
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitFunc_decl(PLCSTPARSERParser.Func_declContext ctx) {
        TranslateFunc_decl translateFunc_decl = new TranslateFunc_decl();
        translateFunc_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitClass_decl(PLCSTPARSERParser.Class_declContext ctx) {
        TranslateClass_decl translateClass_decl = new TranslateClass_decl();
        translateClass_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitData_type_decl(PLCSTPARSERParser.Data_type_declContext ctx) {
        TranslateData_type_decl translateData_type_decl = new TranslateData_type_decl();
        translateData_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitType_decl(PLCSTPARSERParser.Type_declContext ctx) {
        TranslateType_decl translateType_decl = new TranslateType_decl();
        translateType_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译数组类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitArray_type_decl(PLCSTPARSERParser.Array_type_declContext ctx) {
        TranslateArray_type_decl translateArray_type_decl = new TranslateArray_type_decl();
        translateArray_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitDerived_type_decl(PLCSTPARSERParser.Derived_type_declContext ctx) {
        TranslateDerived_type_decl translateDerived_type_decl = new TranslateDerived_type_decl();
        translateDerived_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译枚举类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx) {
        TranslateEnum_type_decl translateEnum_type_decl = new TranslateEnum_type_decl();
        translateEnum_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitSimple_type_decl(PLCSTPARSERParser.Simple_type_declContext ctx) {
        TranslateSimple_type_decl translateSimple_type_decl = new TranslateSimple_type_decl();
        translateSimple_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitStr_type_decl(PLCSTPARSERParser.Str_type_declContext ctx) {
        TranslateStr_type_decl translateStr_type_decl = new TranslateStr_type_decl();
        translateStr_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译struct类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitStruct_decl(PLCSTPARSERParser.Struct_declContext ctx) {
        TranslateStruct_decl translateStruct_decl = new TranslateStruct_decl();
        translateStruct_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译struct类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx) {
        TranslateStruct_elem_decl translateStruct_elem_decl = new TranslateStruct_elem_decl();
        translateStruct_elem_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译struct类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitStruct_spec(PLCSTPARSERParser.Struct_specContext ctx) {
        TranslateStruct_spec translateStruct_spec = new TranslateStruct_spec();
        translateStruct_spec.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译struct类型声明
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx) {
//        TranslateStruct_type_decl translateStruct_type_decl = new TranslateStruct_type_decl();
//        translateStruct_type_decl.translateNode(ctx, this);
        return null;
    }

    /**
     * 翻译函数调用
     * @param ctx the parse tree
     * @return null
     */
    @Override public ArrayList<String> visitFunc_call(PLCSTPARSERParser.Func_callContext ctx) {
        TranslateFunc_call translateFunc_call = new TranslateFunc_call();
        translateFunc_call.translateNode(ctx, this);
        return null;
    }



}
