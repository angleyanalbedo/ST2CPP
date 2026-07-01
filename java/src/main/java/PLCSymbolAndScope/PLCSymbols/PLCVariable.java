package PLCSymbolAndScope.PLCSymbols;

import PLCSymbolAndScope.PLCScopeStack;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class PLCVariable extends PLCSymbol{
    /*
    * variable中的几个属性都需要手动设置
    * */

    public String getUniqueName() {
        return this.name;
    }

    public String getUniqueNameOfInstance(int instanceSymbolId){
        return this.name;
    }

    //变量段类型
    public PLCModifierEnum.VarSections varSections = PLCModifierEnum.VarSections.VAR;

    //访问权限
    public PLCModifierEnum.AccessModifier accessModifier = PLCModifierEnum.AccessModifier.NOT_DECLARED;

    //是否为保留变量
    public PLCModifierEnum.RetainModifier retainQualifiers = PLCModifierEnum.RetainModifier.NON_RETAIN;

    //是否是常量
    public boolean ifConst = false;

    //在声明时变量被赋予的初始值
    // 或者是expr产生的临时变量的值
    //默认为empty
    private String assignVar = "";

    // ─── 结构化初始化信息 ───
    public enum InitKind { NONE, SIMPLE, AGGREGATE, ARRAY }
    private InitKind initKind = InitKind.NONE;
    private String simpleInitValue = null;                    // SIMPLE: "0", "TRUE", "\"str\""
    private LinkedHashMap<String, String> namedInit = null;   // AGGREGATE: {J1: "0.0", J2: "-45.0"}

    // ─── 结构化函数调用信息 ───
    public enum FuncCallKind { NONE, FB_CALL, FUNC_CALL, METHOD_CALL }
    private FuncCallKind funcCallKind = FuncCallKind.NONE;
    private String funcCallName = null;        // 函数名
    private String funcCallInstance = null;    // CLASS 实例名（METHOD_CALL 时）
    private java.util.List<String> funcCallArgs = null;  // 参数列表

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    //locate at
    private String location = "";

    // ─── 初始化信息 getter/setter ───
    public InitKind getInitKind() { return initKind; }
    public String getSimpleInitValue() { return simpleInitValue; }
    public LinkedHashMap<String, String> getNamedInit() { return namedInit; }

    public void setSimpleInit(String value) {
        this.initKind = InitKind.SIMPLE;
        this.simpleInitValue = value;
    }

    public void setAggregateInit(LinkedHashMap<String, String> namedValues) {
        this.initKind = InitKind.AGGREGATE;
        this.namedInit = namedValues;
    }

    public void setArrayInit() {
        this.initKind = InitKind.ARRAY;
    }

    // ─── 函数调用信息 getter/setter ───
    public FuncCallKind getFuncCallKind() { return funcCallKind; }
    public String getFuncCallName() { return funcCallName; }
    public String getFuncCallInstance() { return funcCallInstance; }
    public java.util.List<String> getFuncCallArgs() { return funcCallArgs; }

    public void setFuncCallInfo(FuncCallKind kind, String name, String instance, java.util.List<String> args) {
        this.funcCallKind = kind;
        this.funcCallName = name;
        this.funcCallInstance = instance;
        this.funcCallArgs = args;
    }

    // ─── 解析辅助方法 ───

    /** 从 assignVar 提取函数名：*FUNC_NAME(...) → FUNC_NAME */
    public String extractFuncName() {
        if (assignVar == null) return "";
        String cleaned = assignVar.startsWith("*") ? assignVar.substring(1) : assignVar;
        int parenIdx = cleaned.indexOf('(');
        return parenIdx > 0 ? cleaned.substring(0, parenIdx) : cleaned;
    }

    /** 去除外层括号：(expr) → expr */
    public static String stripParens(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")")) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /** 判断是否为命名聚合初始化：含 := 且有括号 */
    public boolean isAggregateInit() {
        return assignVar != null && assignVar.contains(":=") && assignVar.contains("(");
    }

    /** 解析命名聚合初始化：(J1:=0.0,J2:=-45.0) → {J1: "0.0", J2: "-45.0"} */
    public LinkedHashMap<String, String> parseNamedInit() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (!isAggregateInit()) return result;
        String inner = stripParens(assignVar);
        if (inner.isEmpty()) return result;
        for (String part : inner.split(",")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf(":=");
            if (eq >= 0) {
                result.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 2).trim());
            }
        }
        return result;
    }

    public void setIfConst(boolean ifConst){
        this.ifConst = ifConst;
    }

    public boolean getIfConst(){
        return this.ifConst;
    }

    public void setVarSections(PLCModifierEnum.VarSections varSections) {
        this.varSections = varSections;
    }

    public PLCModifierEnum.VarSections getVarSections() {
        return varSections;
    }

    public void setRetainQualifiers(PLCModifierEnum.RetainModifier retainQualifiers) {
        this.retainQualifiers = retainQualifiers;
    }

    public void setRetainQualifiers(String retainQualifiers) {
        this.retainQualifiers = PLCModifierEnum.RetainModifier.valueOf(retainQualifiers);
    }

    public PLCModifierEnum.RetainModifier getRetainQualifiers() {
        return retainQualifiers;
    }

    public void setAccessModifier(PLCModifierEnum.AccessModifier accessModifier) {
        this.accessModifier = accessModifier;
    }

    public PLCModifierEnum.AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public PLCTypeDeclSymbol getDeclSymbol() {
        return declSymbol;
    }

    public void setDeclSymbol(PLCTypeDeclSymbol declSymbol) {
        this.declSymbol = declSymbol;
    }

    //变量符号对应的声明的符号
    private PLCTypeDeclSymbol declSymbol;

    // 数组变量的维度上下界信息，每个维度存 [lower, upper, count]
    // 例如 ARRAY[0..5] OF INT → arrayBounds = [[0, 5, 6]]
    // 例如 ARRAY[1..10, 0..3] → arrayBounds = [[1, 10, 10], [0, 3, 4]]
    private int[][] arrayBounds;

    public int[][] getArrayBounds() { return arrayBounds; }
    public void setArrayBounds(int[][] bounds) { this.arrayBounds = bounds; }

    /** 获取数组总元素个数（所有维度 count 的乘积），非数组返回 0 */
    public int getArrayTotalCount() {
        if (arrayBounds == null) return 0;
        int total = 1;
        for (int[] dim : arrayBounds) {
            total *= dim[2]; // count = upper - lower + 1
        }
        return total;
    }

    public PLCVariable(){
        super();
    }

    public PLCVariable(PLCVariable another) {
        this.typeId = another.typeId;
        this.sort = another.sort;
        this.varSections = another.varSections;
        this.accessModifier = another.accessModifier;
        this.retainQualifiers = another.retainQualifiers;
        this.ifConst = another.ifConst;
        this.assignVar = another.assignVar;
        this.location = another.location;
        this.declSymbol = another.declSymbol;
        this.arrayBounds = another.arrayBounds;
        // 结构化字段
        this.initKind = another.initKind;
        this.simpleInitValue = another.simpleInitValue;
        this.namedInit = another.namedInit;
        this.funcCallKind = another.funcCallKind;
        this.funcCallName = another.funcCallName;
        this.funcCallInstance = another.funcCallInstance;
        this.funcCallArgs = another.funcCallArgs;
    }

    //
    public String getAssignVar() {
        return assignVar;
    }

    public void setAssignVar(String assignVar) {
        this.assignVar = assignVar;
    }


//    @Override
//    public void setName(String name) {
//        super.setName(name);
//
//        PLCModifierEnum.Sort currentScopeSort = PLCScopeStack.currentScope.getDeclSymbol().getSort();
//        switch (currentScopeSort){
//            case FC_DECL, METHOD_DECL -> {
//                this.runtimeName = "* " + PLCScopeStack.currentScope.getScopeLocation() + "->"+name;
//            }
//            default -> {
//                this.runtimeName = "* " + name;
//            }
//        }
//    }

    @Override
    public String toString() {
        return "PLCVariable{" +
                "varSections=" + varSections +
                ", accessModifier=" + accessModifier +
                ", retainQualifiers=" + retainQualifiers +
                ", ifConst=" + ifConst +
                ", assignVar='" + assignVar + '\'' +
                ", location='" + location + '\'' +
                ", symbolId=" + symbolId +
                ", typeId=" + typeId +
                ", name='" + name + '\'' +
                ", rowNum=" + rowNum +
                ", columnNum=" + columnNum +
                ", sort=" + sort +
                ", runtimeName='" + runtimeName + '\'' +
                ", runtimeTypeName='" + runtimeTypeName + '\'' +
                '}';
    }
    public JsonElement toStringJson() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("varSections", varSections.name());
        jsonObject.addProperty("accessModifier", accessModifier.name());
        jsonObject.addProperty("retainQualifiers", retainQualifiers.name());
        jsonObject.addProperty("ifConst", ifConst);
        jsonObject.addProperty("assignVar", assignVar);
        jsonObject.addProperty("location", location);
        jsonObject.addProperty("symbolId", symbolId);
        jsonObject.addProperty("typeId", typeId);
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("rowNum", rowNum);
        jsonObject.addProperty("columnNum", columnNum);
        jsonObject.addProperty("sort", sort!=null?sort.name():null);
        jsonObject.addProperty("runtimeName", runtimeName);
        jsonObject.addProperty("runtimeTypeName", runtimeTypeName);
        JsonObject jsonSymbol = new JsonObject();
        jsonSymbol.add("PLCVariable",jsonObject);
        return jsonSymbol;
    }

//    @Override
//    public String getRuntimeName() {
//        return "*" + getName();
//    }
}
