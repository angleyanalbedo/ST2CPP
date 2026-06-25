package PLCTranslator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flat 后端 — 生成 GVL 偏移量风格的 C++ 代码
 *
 * 使用新版运行时 rt_plc.h + rt_runtime.h，
 * 变量通过偏移量直接读写 GVL 内存，零堆分配、零虚函数。
 *
 * 所有 emit 方法返回代码字符串（不再直接写文件）。
 * 所有接收表达式参数的 emit 方法会自动调用 translateExpr() 将
 * OOP 风格的 assignVar 转换为原生 C++ 表达式。
 */
public class FlatCodeGenerator implements CodeGenerator {

    // GVL 偏移量分配
    private final Map<String, Integer> offsetMap = new LinkedHashMap<>();
    private final Map<String, String> typeMap = new LinkedHashMap<>();
    private int currentOffset = 0;

    // symbolId → 变量名 映射（用于表达式转换时查找变量名）
    private final Map<String, String> symbolIdToNameMap = new LinkedHashMap<>();

    // 变量名 → symbolId 映射（反向查找）
    private final Map<String, String> nameToSymbolIdMap = new LinkedHashMap<>();

    // ─── Struct 类型支持 ───

    /**
     * Struct 字段信息
     */
    public static class StructField {
        String name;
        String type;
        int offset;
        public StructField(String name, String type, int offset) {
            this.name = name; this.type = type; this.offset = offset;
        }
    }

    /**
     * Struct 布局信息
     */
    public static class StructLayout {
        String structName;
        List<StructField> fields;
        int totalSize;
        public StructLayout(String structName, List<StructField> fields, int totalSize) {
            this.structName = structName;
            this.fields = fields;
            this.totalSize = totalSize;
        }
    }

    // struct 类型名 → 布局（如 "MyStruct" → layout）
    private final Map<String, StructLayout> structLayoutMap = new HashMap<>();

    // PLC_Struct_Value<ID> → struct 类型名映射
    private final Map<String, String> oopStructTypeToName = new HashMap<>();

    // ─── FOR 循环 GVL 局部变量遮盖 ───
    // 被 FOR 循环局部变量遮盖的 GVL 变量名集合（O(1) 查询用）
    private final Set<String> shadowedGvlVars = new HashSet<>();
    // 嵌套顺序栈，内含变量名/类型/偏移
    private static class ShadowEntry {
        String varName; String type; int offset;
        ShadowEntry(String v, String t, int o) { varName = v; type = t; offset = o; }
    }
    private final ArrayDeque<ShadowEntry> shadowStack = new ArrayDeque<>();

    /**
     * 注册一个 struct 类型
     * @param typeName    C++ struct 名（如 "MyStruct"）
     * @param oopTypeName PLC_Struct_Value<ID> 字符串
     * @param layout      struct 布局信息
     */
    public void registerStructType(String typeName, String oopTypeName, StructLayout layout) {
        structLayoutMap.put(typeName, layout);
        // 注册 OOP 类型名 → C++ struct 名的映射
        oopStructTypeToName.put(oopTypeName, typeName);
        // 注册到 SIZE_MAP 用于偏移量计算
        SIZE_MAP.put(typeName, layout.totalSize);
    }

    /**
     * 获取 struct 字段在结构体内的偏移量
     */
    public Integer getStructFieldOffset(String structTypeName, String fieldName) {
        StructLayout layout = structLayoutMap.get(structTypeName);
        if (layout == null) return null;
        for (StructField f : layout.fields) {
            if (f.name.equals(fieldName)) return f.offset;
        }
        return null;
    }

    // ST 类型名 → 原生 C++ 类型名
    private static final Map<String, String> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put("SINT", "SINT");
        TYPE_MAP.put("INT", "INT");
        TYPE_MAP.put("DINT", "DINT");
        TYPE_MAP.put("LINT", "LINT");
        TYPE_MAP.put("USINT", "USINT");
        TYPE_MAP.put("UINT", "UINT");
        TYPE_MAP.put("UDINT", "UDINT");
        TYPE_MAP.put("ULINT", "ULINT");
        TYPE_MAP.put("REAL", "REAL");
        TYPE_MAP.put("LREAL", "LREAL");
        TYPE_MAP.put("BOOL", "BOOL");
        TYPE_MAP.put("STRING", "STRING");
        TYPE_MAP.put("TIME", "TIME");
        TYPE_MAP.put("DATE", "DATE");
        TYPE_MAP.put("TIME_OF_DAY", "TIME_OF_DAY");
        TYPE_MAP.put("DATE_AND_TIME", "DATE_AND_TIME");
        // PLC_Value 风格的类型名也映射
        TYPE_MAP.put("PLC_SINT_Value", "SINT");
        TYPE_MAP.put("PLC_INT_Value", "INT");
        TYPE_MAP.put("PLC_DINT_Value", "DINT");
        TYPE_MAP.put("PLC_LINT_Value", "LINT");
        TYPE_MAP.put("PLC_Real_Value", "REAL");
        TYPE_MAP.put("PLC_LReal_Value", "LREAL");
        TYPE_MAP.put("PLC_Bool_Value", "BOOL");
        TYPE_MAP.put("PLC_String_Value", "STRING");
    }

    // 类型 → 字节大小
    private static final Map<String, Integer> SIZE_MAP = new HashMap<>();
    static {
        SIZE_MAP.put("SINT", 1);
        SIZE_MAP.put("INT", 2);
        SIZE_MAP.put("DINT", 4);
        SIZE_MAP.put("LINT", 8);
        SIZE_MAP.put("USINT", 1);
        SIZE_MAP.put("UINT", 2);
        SIZE_MAP.put("UDINT", 4);
        SIZE_MAP.put("ULINT", 8);
        SIZE_MAP.put("REAL", 4);
        SIZE_MAP.put("LREAL", 8);
        SIZE_MAP.put("BOOL", 1);
        SIZE_MAP.put("STRING", 256);
        SIZE_MAP.put("TIME", 8);
        SIZE_MAP.put("DATE", 4);
        SIZE_MAP.put("TIME_OF_DAY", 8);
        SIZE_MAP.put("DATE_AND_TIME", 8);
    }

    public String toNativeType(String typeName) {
        if (typeName == null) return typeName;
        // 优先查实例级 OOP struct 映射
        String mapped = oopStructTypeToName.get(typeName);
        if (mapped != null) return mapped;
        // 再查静态类型映射
        mapped = TYPE_MAP.get(typeName);
        return mapped != null ? mapped : typeName;
    }

    public int getTypeSize(String nativeType) {
        if (nativeType == null) return 4;
        Integer size = SIZE_MAP.get(nativeType);
        if (size != null) return size;
        // 检查是否已注册的 struct 类型
        StructLayout layout = structLayoutMap.get(nativeType);
        return layout != null ? layout.totalSize : 4;
    }

    /**
     * 为变量分配 GVL 偏移量
     */
    private int allocateOffset(String varName, String nativeType) {
        int size = getTypeSize(nativeType);
        // 对齐到类型大小
        int aligned = (currentOffset + size - 1) / size * size;
        offsetMap.put(varName, aligned);
        typeMap.put(varName, nativeType);
        currentOffset = aligned + size;
        return aligned;
    }

    /**
     * 注册变量名和 symbolId 的映射关系
     * 用于表达式转换时将 RFM symbolId 查找替换为 GVL 偏移量读写
     */
    public void registerVariable(String varName, String symbolId) {
        if (varName != null && symbolId != null) {
            symbolIdToNameMap.put(symbolId, varName);
            nameToSymbolIdMap.put(varName, symbolId);
        }
    }

    /**
     * 根据 symbolId 查找变量名
     */
    private String findVarNameBySymbolId(String symbolId) {
        return symbolIdToNameMap.get(symbolId);
    }

    /**
     * 获取变量的 GVL 读取表达式
     */
    private String readExpr(String varName) {
        // 去掉 OOP 模式的解引用星号前缀
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;

        // 检测数组元素访问：ARR[I] 或 (ARR[I])
        String arrayElemPattern = "^\\(?([A-Z][A-Z0-9$_]*)\\[(.+)\\]\\)?$";
        java.util.regex.Matcher arrayMatcher = java.util.regex.Pattern.compile(arrayElemPattern).matcher(cleanName.trim());
        if (arrayMatcher.matches()) {
            String arrName = arrayMatcher.group(1);
            String indexExpr = arrayMatcher.group(2);
            String arrType = typeMap.get(arrName);
            Integer arrOffset = offsetMap.get(arrName);
            if (arrType != null && arrType.startsWith("ARRAY[") && arrOffset != null) {
                // 解析元素类型和数量：ARRAY[count] OF elemType
                java.util.regex.Pattern typePattern = java.util.regex.Pattern.compile("ARRAY\\[(\\d+)\\] OF (\\w+)");
                java.util.regex.Matcher typeMatcher = typePattern.matcher(arrType);
                if (typeMatcher.find()) {
                    String elemType = typeMatcher.group(2);
                    int count = Integer.parseInt(typeMatcher.group(1));
                    // 生成：gvl.safeArrayAt<ELEM_TYPE>(offset, index, count)
                    return "gvl.safeArrayAt<" + elemType + ">(" + arrOffset + ", " + indexExpr + ", " + count + ")";
                }
            }
        }

        String type = typeMap.get(cleanName);
        Integer offset = offsetMap.get(cleanName);
        if (type == null || offset == null) {
            return varName;
        }
        return "gvl.read<" + type + ">(" + offset + ")";
    }

    /**
     * 获取变量的 GVL 写入语句
     */
    private String writeExpr(String varName, String valueExpr) {
        // 去掉 OOP 模式的解引用星号前缀
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;
        // 处理 OOP 函数返回值：*this->returnValue → return
        if (cleanName.trim().equals("this->returnValue")) {
            return "return " + valueExpr;
        }

        // 检测数组元素访问：ARR[I] 或 (ARR[I])
        String arrayElemPattern = "^\\(?([A-Z][A-Z0-9$_]*)\\[(.+)\\]\\)?$";
        java.util.regex.Matcher arrayMatcher = java.util.regex.Pattern.compile(arrayElemPattern).matcher(cleanName.trim());
        if (arrayMatcher.matches()) {
            String arrName = arrayMatcher.group(1);
            String indexExpr = arrayMatcher.group(2);
            String arrType = typeMap.get(arrName);
            Integer arrOffset = offsetMap.get(arrName);
            if (arrType != null && arrType.startsWith("ARRAY[") && arrOffset != null) {
                // 解析元素类型和数量：ARRAY[count] OF elemType
                java.util.regex.Pattern typePattern = java.util.regex.Pattern.compile("ARRAY\\[(\\d+)\\] OF (\\w+)");
                java.util.regex.Matcher typeMatcher = typePattern.matcher(arrType);
                if (typeMatcher.find()) {
                    String elemType = typeMatcher.group(2);
                    int count = Integer.parseInt(typeMatcher.group(1));
                    // 生成：gvl.safeArrayAt<ELEM_TYPE>(offset, index, count) = value
                    return "gvl.safeArrayAt<" + elemType + ">(" + arrOffset + ", " + indexExpr + ", " + count + ") = " + valueExpr;
                }
            }
        }

        // 检测 struct 字段写入：MY_STRUCT.FIELD = value → gvl.write<FIELD_TYPE>(offset + fieldOffset, value)
        // 同时处理 ARR[I].FIELD → gvl.safeArrayAt<STRUCT>(arrOff, I, cnt).FIELD = value
        String trimmed = cleanName.trim();
        // 去掉外层括号，如 (MY_STRUCT.FIELD) → MY_STRUCT.FIELD
        String stripped = trimmed;
        if (stripped.startsWith("(") && stripped.endsWith(")")) {
            stripped = stripped.substring(1, stripped.length() - 1).trim();
        }
        int dotIdx = stripped.indexOf('.');
        if (dotIdx > 0) {
            String baseVar = stripped.substring(0, dotIdx);
            String fieldPart = stripped.substring(dotIdx + 1);

            // 情况1：baseVar 是简单 GVL 变量（如 MY_STRUCT）
            Integer baseOffset = offsetMap.get(baseVar);
            String baseTypeName = typeMap.get(baseVar);
            if (baseOffset != null && baseTypeName != null) {
                StructLayout layout = structLayoutMap.get(baseTypeName);
                if (layout != null) {
                    for (StructField f : layout.fields) {
                        if (f.name.equals(fieldPart)) {
                            return "gvl.write<" + f.type + ">(" + (baseOffset + f.offset) + ", " + valueExpr + ")";
                        }
                    }
                }
            }

            // 情况2：baseVar 是数组元素访问（如 ARR[I]）
            java.util.regex.Matcher arrBaseMatcher =
                java.util.regex.Pattern.compile("^([A-Z][A-Z0-9$_]*)\\[(.+)\\]$").matcher(baseVar);
            if (arrBaseMatcher.matches()) {
                String arrName = arrBaseMatcher.group(1);
                String indexExpr = arrBaseMatcher.group(2);
                Integer arrOffset = offsetMap.get(arrName);
                String arrType = typeMap.get(arrName);
                if (arrType != null && arrType.startsWith("ARRAY[") && arrOffset != null) {
                    java.util.regex.Matcher typeMatcher =
                        java.util.regex.Pattern.compile("ARRAY\\[(\\d+)\\] OF (\\w+)").matcher(arrType);
                    if (typeMatcher.find()) {
                        String elemType = typeMatcher.group(2);
                        int count = Integer.parseInt(typeMatcher.group(1));
                        StructLayout elemLayout = structLayoutMap.get(elemType);
                        if (elemLayout != null) {
                            for (StructField f : elemLayout.fields) {
                                if (f.name.equals(fieldPart)) {
                                    return "gvl.safeArrayAt<" + elemType + ">(" + arrOffset + ", "
                                        + indexExpr + ", " + count + ")." + fieldPart + " = " + valueExpr;
                                }
                            }
                        }
                    }
                }
            }
        }

        String type = typeMap.get(trimmed);
        Integer offset = offsetMap.get(trimmed);
        if (type == null || offset == null) {
            return trimmed + " = " + valueExpr;
        }
        return "gvl.write<" + type + ">(" + offset + ", " + valueExpr + ")";
    }


    // ═══ 文件头尾 ═══

    @Override
    public String emitHeader() {
        return "// Generated by ST2C++ (Flat backend)\n"
                + "#include \"rt_plc.h\"\n"
                + "#include \"rt_runtime.h\"\n"
                + "\nusing namespace rt_plc;\n\n";
    }

    @Override
    public String emitFooter() {
        return "";
    }


    // ═══ 变量声明 ═══

    @Override
    public String emitVarDecl(String name, String typeName, String assignVar) {
        // 处理数组类型：ARRAY[low..high] OF elemType
        if (typeName != null && typeName.startsWith("ARRAY")) {
            ArrayInfo info = parseArrayType(typeName);
            if (info != null) {
                int elemSize = getTypeSize(info.elemType);
                int totalSize = elemSize * info.count;
                int aligned = (currentOffset + elemSize - 1) / elemSize * elemSize;
                offsetMap.put(name, aligned);
                typeMap.put(name, "ARRAY[" + info.count + "] OF " + info.elemType);
                currentOffset = aligned + totalSize;
                return "";
            }
        }
        String nativeType = toNativeType(typeName);
        allocateOffset(name, nativeType);
        if (assignVar != null && !assignVar.isEmpty() && !"0".equals(assignVar) && !"\"\"".equals(assignVar)) {
            String translated = translateExpr(assignVar);
            return "\n\t\t" + writeExpr(name, translated) + ";";
        }
        return "";
    }

    @Override
    public String emitGlobalVarDecl(String name, String typeName, String assignVar, String varSection) {
        // 处理数组类型
        if (typeName != null && typeName.startsWith("ARRAY")) {
            ArrayInfo info = parseArrayType(typeName);
            if (info != null) {
                int elemSize = getTypeSize(info.elemType);
                int totalSize = elemSize * info.count;
                int aligned = (currentOffset + elemSize - 1) / elemSize * elemSize;
                offsetMap.put(name, aligned);
                typeMap.put(name, "ARRAY[" + info.count + "] OF " + info.elemType);
                currentOffset = aligned + totalSize;
                return "";
            }
        }
        String nativeType = toNativeType(typeName);
        allocateOffset(name, nativeType);
        return "";
    }

    @Override
    public String emitProgVarDecl(String name, String typeName, String assignVar) {
        // 处理数组类型
        if (typeName != null && typeName.startsWith("ARRAY")) {
            ArrayInfo info = parseArrayType(typeName);
            if (info != null) {
                int elemSize = getTypeSize(info.elemType);
                int totalSize = elemSize * info.count;
                int aligned = (currentOffset + elemSize - 1) / elemSize * elemSize;
                offsetMap.put(name, aligned);
                typeMap.put(name, "ARRAY[" + info.count + "] OF " + info.elemType);
                currentOffset = aligned + totalSize;
                return "";
            }
        }
        String nativeType = toNativeType(typeName);
        allocateOffset(name, nativeType);
        return "";
    }

    /**
     * 数组类型信息
     */
    private static class ArrayInfo {
        int count;      // 元素数量
        String elemType; // 元素类型（原生 C++ 类型名）
    }

    /**
     * 解析数组类型字符串，如 "ARRAY[0..4] OF INT"
     * 返回 ArrayInfo，解析失败返回 null
     */
    private ArrayInfo parseArrayType(String typeName) {
        ArrayInfo info = new ArrayInfo();
        // 匹配 ARRAY[low..high] OF elemType
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "ARRAY\\[(\\d+)\\.\\.(\\d+)\\]\\s+OF\\s+(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(typeName);
        if (matcher.find()) {
            int low = Integer.parseInt(matcher.group(1));
            int high = Integer.parseInt(matcher.group(2));
            info.count = high - low + 1;
            info.elemType = toNativeType(matcher.group(3));
            return info;
        }
        return null;
    }


    // ═══ 赋值 ═══

    @Override
    public String emitAssign(String varName, String exprAssignVar) {
        String translated = translateExpr(exprAssignVar);
        return "\n\t\t" + writeExpr(varName, translated) + ";";
    }

    @Override
    public String emitFuncReturnAssign(String exprAssignVar) {
        String translated = translateExpr(exprAssignVar);
        return "\n\t\treturn " + translated + ";";
    }


    // ═══ 控制流 ═══

    @Override
    public String emitIfBegin(String condAssignVar) {
        return "\n\t\tif(" + translateExpr(condAssignVar) + "){";
    }

    @Override
    public String emitElseIf(String condAssignVar) {
        return "\n\t\t}else if(" + translateExpr(condAssignVar) + "){";
    }

    @Override
    public String emitElse() {
        return "\n\t\t}else{";
    }

    @Override
    public String emitIfEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitForBegin(String controlVar, String fromAssignVar, String toAssignVar, String stepAssignVar) {
        // 去掉 OOP 模式的解引用星号前缀
        String cleanVar = controlVar.startsWith("*") ? controlVar.substring(1) : controlVar;
        StringBuilder sb = new StringBuilder();

        // 检查控制变量是否在 GVL 中（已分配偏移量）
        boolean isGvlVar = offsetMap.containsKey(cleanVar);

        if (isGvlVar) {
            // GVL 变量：声明同名的局部变量遮盖 GVL，
            // 从 GVL 读取初值，循环结束后写回终值
            String type = typeMap.getOrDefault(cleanVar, "INT");
            int offset = offsetMap.get(cleanVar);
            // 注册为遮盖变量（translateExpr 跳过 GVL 替换）
            shadowedGvlVars.add(cleanVar);
            shadowStack.addLast(new ShadowEntry(cleanVar, type, offset));
            sb.append("\n\t\t").append(type).append(" ").append(cleanVar).append(" = ")
              .append(translateExpr(fromAssignVar)).append(";");
            sb.append("\n\t\tfor( ; ").append(cleanVar).append(" <= ")
              .append(translateExpr(toAssignVar)).append(";");
        } else {
            // 局部变量：直接在 for 中声明
            sb.append("\n\t\tfor( ").append(cleanVar).append(" = ")
              .append(translateExpr(fromAssignVar)).append(";");
            sb.append(cleanVar).append(" <= ").append(translateExpr(toAssignVar)).append(";");
        }

        if (stepAssignVar != null && !stepAssignVar.isEmpty()) {
            sb.append(cleanVar).append(" = ").append(cleanVar).append(" + ")
              .append(translateExpr(stepAssignVar)).append("){");
        } else {
            sb.append(cleanVar).append("++){");
        }
        return sb.toString();
    }

    @Override
    public String emitForEnd() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t}");
        // 如果栈顶有遮盖变量，写回 GVL 并取消遮盖
        if (!shadowStack.isEmpty()) {
            ShadowEntry entry = shadowStack.removeLast();
            shadowedGvlVars.remove(entry.varName);
            sb.append("\n\t\tgvl.write<").append(entry.type).append(">(")
              .append(entry.offset).append(", ").append(entry.varName).append(");");
        }
        return sb.toString();
    }

    @Override
    public String emitWhileBegin(String condAssignVar) {
        return "\n\t\twhile(" + translateExpr(condAssignVar) + "){";
    }

    @Override
    public String emitWhileEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitRepeatBegin() {
        return "\n\t\tdo{";
    }

    @Override
    public String emitRepeatEnd(String condAssignVar) {
        return "\n\t\t}while(!(" + translateExpr(condAssignVar) + "));";
    }

    @Override
    public String emitCaseBegin(String exprAssignVar) {
        return "\n\t\tswitch(" + translateExpr(exprAssignVar) + "){";
    }

    @Override
    public String emitCaseOption(String value) {
        return "\n\t\t\tcase " + translateExpr(value) + " :";
    }

    @Override
    public String emitCaseDefault() {
        return "\n\t\t\tdefault :";
    }

    @Override
    public String emitCaseEnd() {
        return "\n\t\t}";
    }

    @Override
    public String emitPrintStmt(String exprAssignVar) {
        String translated = translateExpr(exprAssignVar);
        return "\n\t\tprintf(\"%d\\n\", (int)(" + translated + "));";
    }


    // ═══ 函数 / PROGRAM ═══

    @Override
    public String emitFuncDeclBegin(String funcName, String returnType, String params) {
        String nativeReturn = toNativeType(returnType);
        return "\n" + nativeReturn + " " + funcName + "(" + params + ") {";
    }

    @Override
    public String emitFuncDeclEnd() {
        return "\n}";
    }

    @Override
    public String emitFuncBodyBegin() {
        return "";
    }

    @Override
    public String emitFuncBodyEnd() {
        return "";
    }

    @Override
    public String emitFuncCall(String funcName, List<String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(funcName).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(translateExpr(params.get(i)));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String emitProgDeclBegin(String progName) {
        return "\nvoid PROGRAM_" + progName + "(GVL& gvl, ProcessImage& io, TIME dt) {";
    }

    @Override
    public String emitProgDeclEnd() {
        return "\n}";
    }

    @Override
    public String emitProgBodyBegin() {
        return "";
    }

    @Override
    public String emitProgBodyEnd() {
        return "";
    }


    // ═══ 函数块 ═══

    @Override
    public String emitFBBodyBegin() {
        return "";
    }

    @Override
    public String emitFBBodyEnd() {
        return "";
    }


    // ═══ 初始化 ═══

    @Override
    public String emitInitFuncBegin() {
        return "\n// Flat mode: variables are in GVL, no initFunc needed\n";
    }

    @Override
    public String emitInitFuncEnd() {
        return "";
    }

    @Override
    public String emitInitFuncCall(String sentence) {
        return "";
    }


    // ═══ 表达式转换 ═══

    /**
     * OOP→Flat 表达式转换器
     *
     * 将静态检查层生成的 OOP 风格 assignVar 转换为原生 C++ 表达式。
     *
     * 转换规则：
     * 1. 字面量：(*(new INT(2))) → (2)
     * 2. 变量引用：(*::PLC::RFM->getSymbolByID<TYPE*>(id)) → gvl.read<TYPE>(offset) 或 varName
     * 3. 函数调用：*::PLC::RFM->getSymbolByID<FUN*>(id)->callFunc(&p1, &p2) → FUN(p1, p2)
     * 4. 清理残余的解引用星号
     */
    private static final Pattern LITERAL_PATTERN =
            Pattern.compile("\\(\\*\\(\\s*new\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\)\\s*\\)");

    private static final Pattern RFM_VAR_PATTERN =
            Pattern.compile("\\(\\*::PLC::RFM->getSymbolByID<(\\w+)\\*>\\s*\\(\\s*(\\d+)\\s*\\)\\s*\\)");

    private static final Pattern RFM_CALL_PATTERN =
            Pattern.compile("\\*::PLC::RFM->getSymbolByID<(\\w+)\\*>\\s*\\(\\s*(\\d+)\\s*\\)->callFunc\\s*\\(([^)]*)\\)");

    private static final Pattern RFM_VAR_SIMPLE_PATTERN =
            Pattern.compile("::PLC::RFM->getSymbolByID<(\\w+)\\*>\\s*\\(\\s*(\\d+)\\s*\\)");

    @Override
    public String translateExpr(String oopExpr) {
        if (oopExpr == null || oopExpr.isEmpty()) {
            return oopExpr;
        }

        String result = oopExpr;
        boolean changed = true;
        int maxRounds = 10;
        while (changed && maxRounds-- > 0) {
            changed = false;
            String prev = result;

            // 1. 字面量：(*(new INT(2))) → (2)
            Matcher litMatcher = LITERAL_PATTERN.matcher(result);
            StringBuilder sb = new StringBuilder();
            while (litMatcher.find()) {
                changed = true;
                String type = litMatcher.group(1);
                String value = litMatcher.group(2).trim();
                if ("BOOL".equals(type)) {
                    value = value.equals("TRUE") || value.equals("1") ? "true" : "false";
                }
                litMatcher.appendReplacement(sb, "(" + value + ")");
            }
            litMatcher.appendTail(sb);
            result = sb.toString();

            // 2. RFM 变量引用（带括号）
            Matcher varMatcher = RFM_VAR_PATTERN.matcher(result);
            sb = new StringBuilder();
            while (varMatcher.find()) {
                changed = true;
                String type = varMatcher.group(1);
                String symbolId = varMatcher.group(2);
                String nativeType = toNativeType(type);
                String varName = findVarNameBySymbolId(symbolId);
                if (varName != null) {
                    Integer offset = offsetMap.get(varName);
                    String nType = typeMap.getOrDefault(varName, nativeType);
                    if (offset != null) {
                        varMatcher.appendReplacement(sb, "gvl.read<" + nType + ">(" + offset + ")");
                    } else {
                        varMatcher.appendReplacement(sb, varName);
                    }
                } else {
                    varMatcher.appendReplacement(sb, "gvl.read<" + nativeType + ">(" + symbolId + ")");
                }
            }
            varMatcher.appendTail(sb);
            result = sb.toString();

            // 3. RFM 函数调用
            Matcher callMatcher = RFM_CALL_PATTERN.matcher(result);
            sb = new StringBuilder();
            while (callMatcher.find()) {
                changed = true;
                String funcType = callMatcher.group(1);
                String params = callMatcher.group(3);
                params = params.replaceAll("&", "").trim();
                callMatcher.appendReplacement(sb, funcType + "(" + params + ")");
            }
            callMatcher.appendTail(sb);
            result = sb.toString();

            // 4. 简单 RFM 变量引用（无括号）
            Matcher simpleVarMatcher = RFM_VAR_SIMPLE_PATTERN.matcher(result);
            sb = new StringBuilder();
            while (simpleVarMatcher.find()) {
                changed = true;
                String type = simpleVarMatcher.group(1);
                String symbolId = simpleVarMatcher.group(2);
                String varName = findVarNameBySymbolId(symbolId);
                if (varName != null) {
                    Integer offset = offsetMap.get(varName);
                    String nType = typeMap.getOrDefault(varName, toNativeType(type));
                    if (offset != null) {
                        simpleVarMatcher.appendReplacement(sb, "gvl.read<" + nType + ">(" + offset + ")");
                    } else {
                        simpleVarMatcher.appendReplacement(sb, varName);
                    }
                } else {
                    simpleVarMatcher.appendReplacement(sb, symbolId);
                }
            }
            simpleVarMatcher.appendTail(sb);
            result = sb.toString();

            // 5. 数组元素访问替换：ARR[I] → *(gvl.ptr<ELEM_TYPE>(offset) + I)
            String arrayAccessPattern = "([A-Z][A-Z0-9$_]*)\\[(.*?)\\]";
            Matcher arrayAccessMatcher = Pattern.compile(arrayAccessPattern).matcher(result);
            sb = new StringBuilder();
            while (arrayAccessMatcher.find()) {
                changed = true;
                String arrName = arrayAccessMatcher.group(1);
                String indexExpr = arrayAccessMatcher.group(2);
                String arrType = typeMap.get(arrName);
                Integer arrOffset = offsetMap.get(arrName);
                if (arrType != null && arrType.startsWith("ARRAY[") && arrOffset != null) {
                    Pattern typePattern = Pattern.compile("ARRAY\\[(\\d+)\\] OF (\\w+)");
                    Matcher typeMatcher = typePattern.matcher(arrType);
                    if (typeMatcher.find()) {
                        String elemType = typeMatcher.group(2);
                        int count = Integer.parseInt(typeMatcher.group(1));
                        // 使用 gvl.safeArrayAt 进行越界检查
                        arrayAccessMatcher.appendReplacement(sb,
                            "gvl.safeArrayAt<" + elemType + ">(" + arrOffset + ", " + indexExpr + ", " + count + ")");
                        continue;
                    }
                }
                arrayAccessMatcher.appendReplacement(sb, arrayAccessMatcher.group(0));
            }
            arrayAccessMatcher.appendTail(sb);
            result = sb.toString();

            if (!changed && result.equals(prev)) break;
        }

        // 6. 清理残余的 OOP 解引用星号
        // OOP 模式下变量是指针，使用 *varName 解引用
        // Flat 模式下变量是值类型，不需要解引用
        // 匹配独立的 *varName（不在运算符上下文中）
        // 策略：替换 (*varName) 为 (varName)，以及独立的 *varName 为 varName
        result = result.replaceAll("\\(\\*([A-Za-z_]\\w*)\\)", "($1)");
        // 替换独立的 *varName（不在其他字符旁边）
        result = result.replaceAll("(?<![\\w*+\\-/<>])\\*([A-Za-z_]\\w*)(?![\\w*+\\-/<>])", "$1");
        // 清理 ( 后面紧跟的 *
        result = result.replace("( *", "(");

        // 6. 清理 OOP 运行时特有的表达式
        // *this->returnValue → returnValue（Flat 模式下用局部变量替代）
        result = result.replace("*this->returnValue", "returnValue");

        // 7. GVL 变量替换：简单变量名 → gvl.read<TYPE>(offset)
        // 遍历所有已注册的 GVL 变量，在表达式中替换为偏移量读取
        // 注意：只替换独立的变量名（不在其他标识符内部）
        // 跳过数组类型变量（数组元素访问需要特殊处理）
        for (Map.Entry<String, Integer> entry : offsetMap.entrySet()) {
            String varName = entry.getKey();
            Integer offset = entry.getValue();
            String type = typeMap.get(varName);
            if (type == null || offset == null) continue;
            // 跳过数组类型变量（类型名以 "ARRAY[" 开头）
            if (type.startsWith("ARRAY[")) continue;
            // 跳过被 FOR 循环局部变量遮盖的 GVL 变量（循环体内引用指向局部变量）
            if (shadowedGvlVars.contains(varName)) continue;
            // 使用正则匹配独立的变量名（前后不是字母/数字/下划线）
            String regex = "(?<![A-Za-z0-9_])" + Pattern.quote(varName) + "(?![A-Za-z0-9_])";
            result = result.replaceAll(regex, "gvl.read<" + type + ">(" + offset + ")");
        }

        return result;
    }


    // ═══ 底层输出 ═══

    @Override
    public String write(String code) {
        return code;
    }


    // ═══ 辅助方法 ═══

    public String getOffsetDefinitions() {
        StringBuilder sb = new StringBuilder();
        sb.append("// GVL Offset Definitions\n");
        for (Map.Entry<String, Integer> entry : offsetMap.entrySet()) {
            String type = typeMap.get(entry.getKey());
            sb.append("// ").append(entry.getKey())
              .append(" : ").append(type)
              .append(" @ offset ").append(entry.getValue())
              .append("\n");
        }
        sb.append("// Total GVL usage: ").append(currentOffset).append(" bytes\n");
        return sb.toString();
    }

    public Map<String, Integer> getOffsetMap() {
        return Collections.unmodifiableMap(offsetMap);
    }

    public Map<String, String> getTypeMap() {
        return Collections.unmodifiableMap(typeMap);
    }
}
