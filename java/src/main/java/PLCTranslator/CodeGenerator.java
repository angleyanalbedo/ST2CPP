package PLCTranslator;

import java.util.List;
import java.util.Map;

/**
 * 代码生成器接口 — Flat 后端
 *
 * 生成 GVL 偏移量风格代码（生产/性能用）。
 * 所有 emit 方法返回生成的代码字符串（而非直接写文件），
 * 由顶层调度器统一收集后一次性写入文件。
 */
public interface CodeGenerator {

    // ═══ 文件头尾 ═══

    /** 生成 #include 和 using 声明 */
    String emitHeader();

    /** 生成文件尾（关闭 writer 等） */
    String emitFooter();


    // ═══ 变量声明 ═══

    /**
     * 生成局部变量声明
     */
    String emitVarDecl(String name, String typeName, String assignVar);

    /**
     * 生成全局变量声明
     */
    String emitGlobalVarDecl(String name, String typeName, String assignVar, String varSection);

    /**
     * 生成 PROGRAM 内变量声明
     */
    String emitProgVarDecl(String name, String typeName, String assignVar);


    // ═══ 赋值 ═══

    /**
     * 生成变量赋值
     */
    String emitAssign(String varName, String exprAssignVar);

    /**
     * 生成函数返回值赋值
     */
    String emitFuncReturnAssign(String exprAssignVar);


    // ═══ 控制流 ═══

    String emitIfBegin(String condAssignVar);
    String emitElseIf(String condAssignVar);
    String emitElse();
    String emitIfEnd();

    String emitForBegin(String controlVar, String fromAssignVar, String toAssignVar, String stepAssignVar);
    String emitForEnd();

    String emitWhileBegin(String condAssignVar);
    String emitWhileEnd();

    String emitRepeatBegin();
    String emitRepeatEnd(String condAssignVar);

    String emitCaseBegin(String exprAssignVar);
    String emitCaseOption(String value);
    String emitCaseDefault();
    String emitCaseEnd();

    String emitPrintElement(String translatedExpr, boolean isString);

    String emitAssert(String condAssignVar, String sourceExpr, int line);


    // ═══ 函数 / PROGRAM ═══

    /**
     * 生成函数声明开始
     */
    String emitFuncDeclBegin(String funcName, String returnType, String params);

    /** 生成函数声明结束 */
    String emitFuncDeclEnd();

    /**
     * 生成函数体开始（包含局部变量初始化）
     */
    String emitFuncBodyBegin();

    /** 生成函数体结束（包含返回值输出） */
    String emitFuncBodyEnd();

    /**
     * 生成函数调用
     */
    String emitFuncCall(String funcName, List<String> params);

    /**
     * 生成 PROGRAM 声明开始
     */
    String emitProgDeclBegin(String progName);

    /** 生成 PROGRAM 声明结束 */
    String emitProgDeclEnd();

    /** 生成 PROGRAM 体开始（变量声明之后，语句之前） */
    String emitProgBodyBegin();

    /** 生成 PROGRAM 体结束 */
    String emitProgBodyEnd();

    // ═══ PROGRAM 生命周期回调 ═══

    /** 生成 Init 回调开始（变量初始化） */
    String emitProgInitBegin(String progName);

    /** 生成 Cyclic 回调开始（循环体） */
    String emitProgCyclicBegin(String progName);

    /** 生成 Pre 回调开始（首次扫描前） */
    String emitProgPreBegin(String progName);

    /** 生成 Post 回调开始（停止时） */
    String emitProgPostBegin(String progName);

    /** 生成回调函数结束 */
    String emitProgFuncEnd();


    // ═══ 函数块 ═══

    String emitFBBodyBegin();
    String emitFBBodyEnd();

    /**
     * 生成 FB 实例调用代码。
     * 将输入参数写入 FB 实例的 struct 字段，然后调用 update()。
     *
     * @param fbInstanceName FB 实例变量名
     * @param fbTypeName     FB 类型名（C++ struct 名）
     * @param paramNames     输入参数名列表（与 FB struct 字段名一致）
     * @param paramValues    输入参数值表达式列表（已翻译为 flat 风格）
     */
    String emitFBCall(String fbInstanceName, String fbTypeName,
                      List<String> paramNames, List<String> paramValues);


    // ═══ 初始化 ═══

    /** 生成 initFunc() 函数（注册所有函数到 RFM） */
    String emitInitFuncBegin();
    String emitInitFuncEnd();
    String emitInitFuncCall(String sentence);


    // ═══ 表达式转换 ═══

    /**
     * 将静态检查层生成的中间表达式转换为 Flat 后端的表达式。
     *
     * 中间表达式示例：
     *   - 字面量：(*(new INT(2))) → 2
     *   - 变量引用：gvl.read<TYPE>(offset)
     *   - 函数调用：FUN(p1, p2)
     *   - 复合表达式：(1 + 2)
     *
     * @param expr 静态检查层生成的中间表达式
     * @return Flat 后端的表达式
     */
    String translateExpr(String expr);


    // ═══ 类型与元数据 ═══

    /** 将 ST 类型名映射为原生 C++ 类型名 */
    String toNativeType(String typeName);

    /** 获取类型的字节大小 */
    int getTypeSize(String nativeType);

    /** 注册一个 PROGRAM 名称（用于 POU 注册） */
    void addProgramName(String name);

    /** 获取变量的原生类型名（如 GVL 中的变量类型） */
    String getVarType(String varName);

    /** 获取当前文件的标识符 */
    String getFileId();

    /** 获取当前文件中所有 PROGRAM 名称 */
    List<String> getProgramNames();

    // ═══ Struct 类型支持 ═══

    /** Struct 字段信息 */
    class StructField {
        public String name;
        public String type;
        public int offset;
        public StructField(String name, String type, int offset) {
            this.name = name; this.type = type; this.offset = offset;
        }
    }

    /** Struct 布局信息 */
    class StructLayout {
        public String structName;
        public List<StructField> fields;
        public int totalSize;
        public StructLayout(String structName, List<StructField> fields, int totalSize) {
            this.structName = structName;
            this.fields = fields;
            this.totalSize = totalSize;
        }
    }

    /** 注册 struct 类型 */
    void registerStructType(String typeName, String runtimeType, StructLayout layout);

    /**
     * 注册 enum 类型，将枚举的运行时类型名（PLC_Enum_Value<ID>）映射到 underlying 原生类型。
     * 用于 toNativeType/getTypeSize 解析 enum 类型变量为 underlying 类型。
     */
    void registerEnumType(String enumName, String runtimeTypeName, String underlyingNativeType);

    /**
     * 生成 enum class 声明。
     * @param enumName 枚举类型名
     * @param underlyingType underlying 原生类型（如 "INT"）
     * @param entries 枚举值条目列表，每个形如 "ValName" 或 "ValName = <literal>"
     */
    String emitEnumDecl(String enumName, String underlyingType, List<String> entries);

    /** 获取变量的 GVL 偏移量 */
    Integer getVarOffset(String varName);

    /** 获取所有变量的偏移量映射（只读） */
    Map<String, Integer> getOffsetMap();

    /** 获取所有变量的类型映射（只读） */
    Map<String, String> getTypeMap();

    /** 注册变量名和 symbolId 的映射关系 */
    void registerVariable(String varName, String symbolId);

    // ═══ POU 注册表（编译器生成元数据，运行时决定调度） ═══

    /**
     * 生成 POU 注册函数，将当前文件中的所有 PROGRAM 注册到 POURegistry。
     *
     * 生成如：
     *   void registerPOU_main(POURegistry& reg) {
     *       reg.add("P", PROGRAM_P);
     *       reg.add("MAIN", PROGRAM_MAIN);
     *   }
     *
     * @param fileId   源文件标识（如 "main"、"io"），用于生成注册函数名
     * @param progNames 当前文件中所有 PROGRAM 名称的列表
     * @return 注册函数的 C++ 代码
     */
    String emitPOURegistration(String fileId, List<String> progNames);


    // ═══ I/O 映射变量支持（AT 地址声明） ═══

    /**
     * 注册一个具有 AT 地址的 I/O 映射变量。
     * 该变量不在 GVL 中分配偏移量，而是映射到 ProcessImage 的输入/输出缓冲区。
     *
     * @param varName  变量名
     * @param typeName 原生类型名
     * @param location AT 地址字符串（如 "%IW2", "%QW4", "%IX0.3"）
     */
    void registerIOVariable(String varName, String typeName, String location);

    /**
     * 检查变量是否为 I/O 映射变量
     */
    boolean isIOVariable(String varName);

    /**
     * 生成 I/O 映射变量的读取表达式
     */
    String emitIORead(String varName);

    /**
     * 生成 I/O 映射变量的写入语句
     */
    String emitIOWrite(String varName, String valueExpr);


    // ═══ 底层输出 ═══

    /** 直接输出代码片段（用于特殊情况） */
    String write(String code);
}
