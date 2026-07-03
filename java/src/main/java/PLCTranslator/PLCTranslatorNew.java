package PLCTranslator;

import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCEnumDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCBaseFUNDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCStructDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCVariable;
import PLCTranslator.TranslateType.Class_decl.TranslateClass_decl;
import PLCTranslator.TranslateType.Expr.TranslateAnd_expr;
import PLCTranslator.TranslateType.Expr.TranslateExpression;
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
import PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Subrange_type_decl.TranslateSubrange_type_decl;
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
import PLCTranslator.TranslateType.TranslateStartpoint;
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
import org.antlr.v4.runtime.ParserRuleContext;

public class PLCTranslatorNew extends PLCSTPARSERBaseVisitor<String> {
    //树节点信息
    static public ParseTreeProperty<java.util.ArrayList<PLCSymbol>> properties = new ParseTreeProperty<>();
    static public GvlContext gvlCtx;

    // 表达式中函数调用产生的临时变量声明，需在语句级统一输出
    public java.util.List<String> pendingDecls = new java.util.ArrayList<>();

    private boolean emitHeader = true;
    private boolean emitPOURegistration = true;

    // 标记当前是否在 PROGRAM cyclic 函数体内（影响变量翻译方式）
    public boolean inCyclic = false;

    // 标记当前是否在 FB 方法体内（使用 gvl.read/write/ptr 而非 gv. 直接访问）
    public boolean inFB = false;

    // 标记当前是否在 CLASS 声明中（生成成员方法 vs 自由函数）
    public boolean inClassDecl = false;

    // 当前 CLASS 名称，用于 SUPER:: 调用查找父类
    public String currentClassName = null;

    // 当前 FC 的返回类型 ID（-1 表示 void），用于 TranslateReturn 判断
    public int currentFuncReturnTypeId = -1;

    // 是否启用 cyclic 局部变量缓存（prologue 加载 / epilogue 写回）
    public boolean localCache = true;

    // 是否输出 #line 指令（GDB 源码级调试）
    private boolean emitLineDirectives = false;
    private String currentSourceName = null;

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

    public void setLocalCache(boolean localCache) {
        this.localCache = localCache;
    }

    public void setEmitLineDirectives(boolean emit) {
        this.emitLineDirectives = emit;
    }

    public void setCurrentSourceName(String name) {
        this.currentSourceName = name;
    }

    /**
     * 在语句级插入 #line 指令，将生成的 C++ 映射回 ST 源码行号。
     * 仅当 emitLineDirectives=true 且 currentSourceName != null 时生效。
     */
    public String lineDirective(ParserRuleContext ctx) {
        if (!emitLineDirectives || currentSourceName == null) return "";
        int line = ctx.getStart().getLine();
        String path = currentSourceName.replace('\\', '/');
        return "\n#line " + line + " \"" + path + "\"\n";
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

    /** @param logicalContext true=逻辑运算(&&/||), false=位运算(&/|) */

    public static String mapOperator(String op) {
        return mapOperator(op, true);
    }

    public static String mapOperator(String op, boolean logicalContext) {
        return switch (op) {
            case "OR" -> logicalContext ? "||" : "|";
            case "XOR" -> "^";
            case "AND" -> logicalContext ? "&&" : "&";
            case "=" -> "==";
            case "<>" -> "!=";
            case "MOD" -> "MOD";
            case "NOT" -> "!";   // TODO: NOT on INT needs ~, NOT on BOOL needs !; 需类型上下文区分
            default -> op;
        };
    }

    // ─── Expression visit overrides ───

    @Override public String visitExpression(PLCSTPARSERParser.ExpressionContext ctx) {
        return new TranslateExpression().translateNode(ctx, this);
    }

    @Override public String visitXor_expr(PLCSTPARSERParser.Xor_exprContext ctx) {
        if (ctx.and_expr().size() == 1) {
            return visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            sb.append(" ^ ").append(visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }

    @Override public String visitAnd_expr(PLCSTPARSERParser.And_exprContext ctx) {
        return new TranslateAnd_expr().translateNode(ctx, this);
    }

    @Override public String visitCompare_expr(PLCSTPARSERParser.Compare_exprContext ctx) {
        if (ctx.equ_expr().size() == 1) {
            return visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            String op = mapOperator(ctx.getChild(i).getText());
            sb.append(" ").append(op).append(" ").append(visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }

    @Override public String visitEqu_expr(PLCSTPARSERParser.Equ_exprContext ctx) {
        if (ctx.add_expr().size() == 1) {
            return visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            String op = mapOperator(ctx.getChild(i).getText());
            sb.append(" ").append(op).append(" ").append(visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }

    @Override public String visitAdd_expr(PLCSTPARSERParser.Add_exprContext ctx) {
        if (ctx.term().size() == 1) {
            return visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            sb.append(" ").append(ctx.getChild(i).getText()).append(" ").append(visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }

    @Override public String visitTerm(PLCSTPARSERParser.TermContext ctx) {
        if (ctx.power_expr().size() == 1) {
            return visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            String op = ctx.getChild(i).getText();
            if ("MOD".equals(op)) {
                sb.insert(0, "MOD(");
                sb.append(", ").append(visit(ctx.getChild(i + 1))).append(")");
                break;
            }
            sb.append(" ").append(op).append(" ").append(visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }

    @Override public String visitPower_expr(PLCSTPARSERParser.Power_exprContext ctx) {
        if (ctx.unary_expr().size() == 1) {
            return visit(ctx.getChild(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.getChild(0)));
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            sb.append(" ").append(ctx.getChild(i).getText()).append(" ").append(visit(ctx.getChild(i + 1)));
        }
        return sb.toString();
    }

    @Override public String visitUnary_expr(PLCSTPARSERParser.Unary_exprContext ctx) {
        return new PLCTranslator.TranslateType.Expr.TranslateUnary_expr().translateNode(ctx, this);
    }

    @Override public String visitPrimary_expr(PLCSTPARSERParser.Primary_exprContext ctx) {
        return new PLCTranslator.TranslateType.Expr.TranslatePrimary_expr().translateNode(ctx, this);
    }

    @Override public String visitVariable_access(PLCSTPARSERParser.Variable_accessContext ctx) {
        return new PLCTranslator.TranslateType.Expr.TranslateVariable_access().translateNode(ctx, this);
    }

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
        String directive = lineDirective(ctx);
        TranslateStmt translateStmt = new TranslateStmt();
        String result = translateStmt.translateNode(ctx, this);
        return directive + (result != null ? result : "");
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
     * 翻译子范围类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitSubrange_type_decl(PLCSTPARSERParser.Subrange_type_declContext ctx) {
        TranslateSubrange_type_decl translateSubrange_type_decl = new TranslateSubrange_type_decl();
        return translateSubrange_type_decl.translateNode(ctx, this);
    }

    /**
     * 翻译枚举类型声明
     * @param ctx the parse tree
     * @return 生成的代码字符串
     */
    @Override public String visitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx) {
        return new PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Enum_type_decl.TranslateEnum_type_decl().translate(ctx, gvlCtx);
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
        return new PLCTranslator.TranslateType.Pou_decl.Data_type_decl.Struct_type_decl.TranslateStruct_type_decl().translate(ctx, gvlCtx);
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
