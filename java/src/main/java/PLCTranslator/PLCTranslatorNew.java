package PLCTranslator;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCStructDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.TranslateType.Class_decl.TranslateClass_decl;
import java.util.List;
import java.util.ArrayList;
import PLCTranslator.TranslateType.Fb_body.TranslateFb_body;
import PLCTranslator.TranslateType.Fb_decl.TranslateFb_decl;
import PLCTranslator.TranslateType.Func_decl.TranslateFunc_decl;
import PLCTranslator.TranslateType.Interface_decl.TranslateInterface_decl;
import PLCTranslator.TranslateType.Method_decl.TranslateMethod_decl;
import PLCTranslator.TranslateType.Method_prototype.TranslateMethod_prototype;
import PLCTranslator.TranslateType.Namespace_decl.TranslateNamespace_decl;
import PLCTranslator.TranslateType.Namespace_decl.TranslateNamespace_elements;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Array_type_decl.TranslateArray_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Derived_type_decl.TranslateDerived_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Simple_type_decl.TranslateSimple_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Str_type_decl.TranslateStr_type_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_elem_decl;
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_spec;
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
import PLCTranslator.TranslateType.Stmt.Assert_stmt.TranslateAssert_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.Case_stmt.TranslateCase_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt.TranslateElsif_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.If_stmt.TranslateIf_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.TranslateElse_stmt;
import PLCTranslator.TranslateType.Stmt.Selection_stmt.TranslateSeletion_stmt;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.CallFunc.TranslateFunc_call;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation.TranslateInvocation1;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.Invocation.TranslateInvocation2;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateCallFunc;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateReturn;
import PLCTranslator.TranslateType.Stmt.Subprog_ctrl_stmt.TranslateSuperCall;
import PLCTranslator.TranslateType.Stmt.TranslateStmt;
import PLCTranslator.TranslateType.Stmt_list.TranslateStmt_list;
import antlr4.PLCSTPARSERBaseVisitor;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class PLCTranslatorNew extends PLCSTPARSERBaseVisitor<String> {
    //树节点信息
    static public ParseTreeProperty<java.util.ArrayList<PLCSymbol>> properties = new ParseTreeProperty<>();
    static public GvlContext gvlCtx;

    private boolean emitHeader = true;
    private boolean emitPOURegistration = true;

    public PLCTranslatorNew(ParseTreeProperty<java.util.ArrayList<PLCSymbol>> properties) {
        PLCTranslatorNew.properties = properties;
        PLCTranslatorNew.gvlCtx = new GvlContext();
    }

    /**
     * 重写 aggregateResult 以拼接所有子节点结果而非仅保留最后一个。
     * ANTLR 默认实现返回 nextResult，导致 visitChildren 中只有末子节点生效。
     * 表达式树中有多个 func_call 时，所有临时变量声明都需要保留。
     */
    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        if (aggregate == null) return nextResult;
        if (nextResult == null) return aggregate;
        return aggregate + nextResult;
    }

    public PLCTranslatorNew(ParseTreeProperty<java.util.ArrayList<PLCSymbol>> properties, GvlContext gvlCtx) {
        PLCTranslatorNew.properties = properties;
        PLCTranslatorNew.gvlCtx = gvlCtx;
    }

    public void setEmitHeader(boolean emitHeader) {
        this.emitHeader = emitHeader;
    }

    public void setEmitPOURegistration(boolean emitPOURegistration) {
        this.emitPOURegistration = emitPOURegistration;
    }

    public boolean shouldEmitHeader() {
        return emitHeader;
    }

    public boolean shouldEmitPOURegistration() {
        return emitPOURegistration;
    }

    /**
     * 安全获取 ParseTreeProperty 中的第一个符号。
     */
    public static PLCSymbol getSymbol(org.antlr.v4.runtime.tree.ParseTree node, String context) {
        java.util.ArrayList<PLCSymbol> symbols = properties.get(node);
        if (symbols == null || symbols.isEmpty()) {
            throw new RuntimeException("Code generation error: no symbols found for " + context);
        }
        return symbols.get(0);
    }

    /**
     * 安全获取 PLCVariable：从 properties 中提取第一个符号并强转为 PLCVariable。
     * 如果符号不存在或类型不匹配，抛出带有上下文信息的异常。
     */
    public static PLCVariable getVariable(org.antlr.v4.runtime.tree.ParseTree node, String context) {
        PLCSymbol sym = getSymbol(node, context);
        if (!(sym instanceof PLCVariable)) {
            throw new RuntimeException("Code generation error: expected PLCVariable for " + context
                    + " but got " + sym.getClass().getSimpleName() + " (" + sym.getName() + ")");
        }
        return (PLCVariable) sym;
    }

    /**
     * 翻译程序初始处
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitStartpoint(PLCSTPARSERParser.StartpointContext ctx) {
        TranslateStartpoint translateStartpoint = new TranslateStartpoint();
        return translateStartpoint.translateNode(ctx, this);
    }

    /**
     * 翻译main函数
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitProg_decl(PLCSTPARSERParser.Prog_declContext ctx) {
        TranslateProg_decl visitProg = new TranslateProg_decl();
        return visitProg.translateNode(ctx, this);
    }

    /**
     * 翻译程序块
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitPou_decl(PLCSTPARSERParser.Pou_declContext ctx) {
        TranslatePou_decl translatePou_decl = new TranslatePou_decl();
        return translatePou_decl.translateNode(ctx, this);
    }

    /**
     * 翻译全局变量
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitGlobal_var_decls(PLCSTPARSERParser.Global_var_declsContext ctx) {
        TranslateGlobal_var_decls translateGlobal_var_decls = new TranslateGlobal_var_decls();
        return translateGlobal_var_decls.translateNode(ctx, this);
    }

    /**
     * 翻译语句列表
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitStmt_list(PLCSTPARSERParser.Stmt_listContext ctx) {
        TranslateStmt_list translateStmt_list = new TranslateStmt_list();
        return translateStmt_list.translateNode(ctx, this);
    }

    /**
     * 翻译语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitStmt(PLCSTPARSERParser.StmtContext ctx) {
        TranslateStmt translateStmt = new TranslateStmt();
        return translateStmt.translateNode(ctx, this);
    }

    /**
     * 翻译引用赋值
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitRef_assign(PLCSTPARSERParser.Ref_assignContext ctx) {
        TranslateRef_assign translateRef_assign = new TranslateRef_assign();
        return translateRef_assign.translateNode(ctx, this);
    }

    /**
     * 翻译引用赋值表达式
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitRefAssignExpression(PLCSTPARSERParser.RefAssignExpressionContext ctx) {
        TranslateRefAssignExpression translateRefAssignExpression = new TranslateRefAssignExpression();
        return translateRefAssignExpression.translateNode(ctx, this);
    }


    /**
     * 翻译变量赋值
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitVariableAssignExpression(PLCSTPARSERParser.VariableAssignExpressionContext ctx) {
        TranslateVariableAssignExpression translateVariableAssignExpression = new TranslateVariableAssignExpression();
        return translateVariableAssignExpression.translateNode(ctx, this);
    }

    /**
     * 翻译循环结构
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitIteration_stmt(PLCSTPARSERParser.Iteration_stmtContext ctx) {
        TranslateIteration_stmt translateIteration_stmt = new TranslateIteration_stmt();
        return translateIteration_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译for循环结构
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitFor_stmt(PLCSTPARSERParser.For_stmtContext ctx) {
        TranslateFor_stmt translateFor_stmt = new TranslateFor_stmt();
        return translateFor_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译repeat循环结构
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitRepeat_stmt(PLCSTPARSERParser.Repeat_stmtContext ctx) {
        TranslateRepeat_stmt translateRepeat_stmt = new TranslateRepeat_stmt();
        return translateRepeat_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译while循环结构
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitWhile_stmt(PLCSTPARSERParser.While_stmtContext ctx) {
        TranslateWhile_stmt translateWhile_stmt = new TranslateWhile_stmt();
        return translateWhile_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译print函数
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitPrint_stmt(PLCSTPARSERParser.Print_stmtContext ctx) {
        TranslatePrint_stmt translatePrint_stmt = new TranslatePrint_stmt();
        return translatePrint_stmt.translateNode(ctx, this);
    }

    @Override
    public String visitAssert_stmt(PLCSTPARSERParser.Assert_stmtContext ctx) {
        TranslateAssert_stmt translateAssert_stmt = new TranslateAssert_stmt();
        return translateAssert_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译选择语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitSelection_stmt(PLCSTPARSERParser.Selection_stmtContext ctx) {
        TranslateSeletion_stmt translateSeletion_stmt = new TranslateSeletion_stmt();
        return translateSeletion_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译else语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitElse_stmt(PLCSTPARSERParser.Else_stmtContext ctx) {
        TranslateElse_stmt translateElse_stmt = new TranslateElse_stmt();
        return translateElse_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译else if语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitElsif_stmt(PLCSTPARSERParser.Elsif_stmtContext ctx) {
        TranslateElsif_stmt translateElsif_stmt = new TranslateElsif_stmt();
        return translateElsif_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译if语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitIf_stmt(PLCSTPARSERParser.If_stmtContext ctx) {
        TranslateIf_stmt translateIf_stmt = new TranslateIf_stmt();
        return translateIf_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译switch case语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitCase_stmt(PLCSTPARSERParser.Case_stmtContext ctx) {
        TranslateCase_stmt translateCase_stmt = new TranslateCase_stmt();
        return translateCase_stmt.translateNode(ctx, this);
    }

    /**
     * 翻译函数调用语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitCallFunc(PLCSTPARSERParser.CallFuncContext ctx) {
        TranslateCallFunc translateCallFunc = new TranslateCallFunc();
        return translateCallFunc.translateNode(ctx, this);
    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
//    @Override
//    public String visitInvocationCall(PLCSTPARSERParser.InvocationCallContext ctx) {
//        TranslateInvocationCall translateInvocationCall = new TranslateInvocationCall();
//        return translateInvocationCall.translateNode(ctx, this);
//    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitInvocation1(PLCSTPARSERParser.Invocation1Context ctx) {
        TranslateInvocation1 translateInvocation1 = new TranslateInvocation1();
        return translateInvocation1.translateNode(ctx, this);
    }

    /**
     * 翻译类实例方法调用语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitInvocation2(PLCSTPARSERParser.Invocation2Context ctx) {
        TranslateInvocation2 translateInvocation2 = new TranslateInvocation2();
        return translateInvocation2.translateNode(ctx, this);
    }

    /**
     * 翻译return语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitReturn(PLCSTPARSERParser.ReturnContext ctx) {
        TranslateReturn translateReturn = new TranslateReturn();
        return translateReturn.translateNode(ctx, this);
    }

    /**
     * 翻译类实例调用父类方法语句
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitSuperCall(PLCSTPARSERParser.SuperCallContext ctx) {
        TranslateSuperCall translateSuperCall = new TranslateSuperCall();
        return translateSuperCall.translateNode(ctx, this);
    }

    /**
     * 翻译命名空间声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitNamespace_decl(PLCSTPARSERParser.Namespace_declContext ctx) {
        TranslateNamespace_decl translateNamespace_decl = new TranslateNamespace_decl();
        return translateNamespace_decl.translateNode(ctx, this);
    }

    /**
     * 翻译命名空间元素
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitNamespace_elements(PLCSTPARSERParser.Namespace_elementsContext ctx) {
        TranslateNamespace_elements translateNamespace_elements = new TranslateNamespace_elements();
        return translateNamespace_elements.translateNode(ctx, this);
    }

    /**
     * 翻译方法原型
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitMethod_prototype(PLCSTPARSERParser.Method_prototypeContext ctx) {
        TranslateMethod_prototype translateMethod_prototype = new TranslateMethod_prototype();
        return translateMethod_prototype.translateNode(ctx, this);
    }

    /**
     * 翻译类内方法声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitMethod_decl(PLCSTPARSERParser.Method_declContext ctx) {
        TranslateMethod_decl translateMethod_decl = new TranslateMethod_decl();
        return translateMethod_decl.translateNode(ctx, this);
    }

    /**
     * 翻译接口声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitInterface_decl(PLCSTPARSERParser.Interface_declContext ctx) {
        TranslateInterface_decl translateInterface_decl = new TranslateInterface_decl();
        return translateInterface_decl.translateNode(ctx, this);
    }

    /**
     * 翻译main函数体
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override
    public String visitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx) {
        TranslateFb_body translateFb_body = new TranslateFb_body();
        return translateFb_body.translateNode(ctx, this);
    }

    /**
     * 翻译函数
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitFunc_decl(PLCSTPARSERParser.Func_declContext ctx) {
        TranslateFunc_decl translateFunc_decl = new TranslateFunc_decl();
        return translateFunc_decl.translateNode(ctx, this);
    }

    /**
     * 翻译功能块
     */
    @Override public String visitFb_decl(PLCSTPARSERParser.Fb_declContext ctx) {
        TranslateFb_decl translateFb_decl = new TranslateFb_decl();
        return translateFb_decl.translateNode(ctx, this);
    }

    /**
     * 翻译类
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitClass_decl(PLCSTPARSERParser.Class_declContext ctx) {
        TranslateClass_decl translateClass_decl = new TranslateClass_decl();
        return translateClass_decl.translateNode(ctx, this);
    }

    /**
     * 翻译数据类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitData_type_decl(PLCSTPARSERParser.Data_type_declContext ctx) {
        TranslateData_type_decl translateData_type_decl = new TranslateData_type_decl();
        return translateData_type_decl.translateNode(ctx, this);
    }

    /**
     * 翻译类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitType_decl(PLCSTPARSERParser.Type_declContext ctx) {
        TranslateType_decl translateType_decl = new TranslateType_decl();
        return translateType_decl.translateNode(ctx, this);
    }

    /**
     * 翻译数组类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitArray_type_decl(PLCSTPARSERParser.Array_type_declContext ctx) {
        TranslateArray_type_decl translateArray_type_decl = new TranslateArray_type_decl();
        return translateArray_type_decl.translateNode(ctx, this);
    }

    /**
     * 翻译派生类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitDerived_type_decl(PLCSTPARSERParser.Derived_type_declContext ctx) {
        TranslateDerived_type_decl translateDerived_type_decl = new TranslateDerived_type_decl();
        return translateDerived_type_decl.translateNode(ctx, this);
    }

    /**
     * 翻译枚举类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx) {
        return translateEnumTypeDeclFlat(ctx, gvlCtx);
    }

    /**
     * 翻译简单类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitSimple_type_decl(PLCSTPARSERParser.Simple_type_declContext ctx) {
        TranslateSimple_type_decl translateSimple_type_decl = new TranslateSimple_type_decl();
        return translateSimple_type_decl.translateNode(ctx, this);
    }

    /**
     * 翻译字符串类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitStr_type_decl(PLCSTPARSERParser.Str_type_declContext ctx) {
        TranslateStr_type_decl translateStr_type_decl = new TranslateStr_type_decl();
        return translateStr_type_decl.translateNode(ctx, this);
    }

    /**
     * 翻译struct类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitStruct_decl(PLCSTPARSERParser.Struct_declContext ctx) {
        TranslateStruct_decl translateStruct_decl = new TranslateStruct_decl();
        return translateStruct_decl.translateNode(ctx, this);
    }

    /**
     * 翻译struct元素声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx) {
        TranslateStruct_elem_decl translateStruct_elem_decl = new TranslateStruct_elem_decl();
        return translateStruct_elem_decl.translateNode(ctx, this);
    }

    /**
     * 翻译struct spec
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitStruct_spec(PLCSTPARSERParser.Struct_specContext ctx) {
        TranslateStruct_spec translateStruct_spec = new TranslateStruct_spec();
        return translateStruct_spec.translateNode(ctx, this);
    }

    /**
     * 翻译struct类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx) {
        return translateStructTypeDeclFlat(ctx, gvlCtx);
    }

    private String translateStructTypeDeclFlat(PLCSTPARSERParser.Struct_type_declContext ctx, GvlContext gvlCtx) {
        PLCStructDeclSymbol structSymbol = (PLCStructDeclSymbol) properties.get(ctx).get(0);
        String structName = structSymbol.getName();
        String runtimeType = "PLC_Struct_Value<" + structSymbol.getTypeId() + ">";

        StringBuilder sb = new StringBuilder();
        sb.append("\nstruct ").append(structName).append(" {");

        List<GvlContext.StructField> fields = new ArrayList<>();
        int currentOffset = 0;

        for (PLCVariable fieldVar : structSymbol.getVariables()) {
            String fieldName = fieldVar.getName();
            String fieldType = gvlCtx.toNativeType(fieldVar.getRuntimeTypeName());
            GvlContext.ArrayInfo arrayInfo = gvlCtx.parseArrayType(fieldVar.getRuntimeTypeName());
            int fieldSize;
            if (arrayInfo != null) {
                fieldSize = arrayInfo.count * gvlCtx.getTypeSize(arrayInfo.elemType);
                int aligned = fieldSize <= 1 ? currentOffset : (currentOffset + fieldSize - 1) / fieldSize * fieldSize;
                fields.add(new GvlContext.StructField(fieldName, fieldType, aligned));
                sb.append("\n    ").append(arrayInfo.elemType).append(" ")
                  .append(fieldName).append("[").append(arrayInfo.count).append("];");
                currentOffset = aligned + fieldSize;
            } else {
                fieldSize = gvlCtx.getTypeSize(fieldType);
                int aligned = fieldSize <= 1 ? currentOffset : (currentOffset + fieldSize - 1) / fieldSize * fieldSize;
                fields.add(new GvlContext.StructField(fieldName, fieldType, aligned));
                sb.append("\n    ").append(fieldType).append(" ").append(fieldName).append(";");
                currentOffset = aligned + fieldSize;
            }
        }

        sb.append("\n};\n");

        GvlContext.StructLayout layout = new GvlContext.StructLayout(
                structName, fields, currentOffset);
        gvlCtx.registerStructType(structName, runtimeType, layout);

        gvlCtx.registerVariable(structName, String.valueOf(structSymbol.getSymbolId()));

        return sb.toString();
    }

    private String translateEnumTypeDeclFlat(PLCSTPARSERParser.Enum_type_declContext ctx, GvlContext gvlCtx) {
        PLCEnumDeclSymbol enumSymbol = (PLCEnumDeclSymbol) properties.get(ctx).get(0);
        String enumName = enumSymbol.getName();

        // 获取 underlying type 的原生 C++ 类型名
        int protoTypeId = enumSymbol.getEnumConstTypeId();
        PLCTypeDeclSymbol protoType = PLCTotalSymbolTable.getTypeByTypeID(protoTypeId);
        String underlyingType = protoType != null ? gvlCtx.toNativeType(protoType.getRuntimeName()) : "INT";

        String runtimeTypeName = "PLC_Enum_Value<" + enumSymbol.getTypeId() + ">";
        gvlCtx.registerEnumType(enumName, runtimeTypeName, underlyingType);

        List<String> entries = new ArrayList<>();
        for (PLCVariable var : enumSymbol.getEnumValues()) {
            String valueExpr = gvlCtx.translateExpr(var.getAssignVar()).trim();
            // 去除外层括号以判断默认值
            String stripped = valueExpr.startsWith("(") && valueExpr.endsWith(")")
                ? valueExpr.substring(1, valueExpr.length() - 1).trim()
                : valueExpr;
            // 如果是默认值 "0" 则省略 = Value，由 C++ 编译器自动编号
            if (stripped.equals("0")) {
                entries.add(var.getName());
            } else {
                entries.add(var.getName() + " = " + valueExpr);
            }
        }

        return gvlCtx.emitEnumDecl(enumName, underlyingType, entries);
    }

    /**
     * 翻译函数调用
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitFunc_call(PLCSTPARSERParser.Func_callContext ctx) {
        TranslateFunc_call translateFunc_call = new TranslateFunc_call();
        return translateFunc_call.translateNode(ctx, this);
    }



}
