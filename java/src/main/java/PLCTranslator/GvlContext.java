package PLCTranslator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GvlContext {

    // GVL 偏移量分配
    public final Map<String, Integer> offsetMap = new LinkedHashMap<>();
    public final Map<String, String> typeMap = new LinkedHashMap<>();
    public int currentOffset = 0;

    // symbolId → 变量名 映射
    public final Map<String, String> symbolIdToNameMap = new LinkedHashMap<>();
    public final Map<String, String> nameToSymbolIdMap = new LinkedHashMap<>();

    // ─── Struct 类型支持 ───
    public final Map<String, StructLayout> structLayoutMap = new HashMap<>();
    public final Map<String, String> structTypeToName = new HashMap<>();

    // ─── Enum 类型支持 ───
    public final Map<String, String> enumRuntimeToUnderlying = new HashMap<>();
    public final Map<String, String> enumNameToUnderlying = new HashMap<>();

    // ─── I/O 映射变量支持 ───
    public enum IODirection { INPUT, OUTPUT, MEMORY }

    public static class IOInfo {
        public IODirection dir;
        public int byteOffset;
        public int bitOffset = -1;
        public String typeName;
        public IOInfo() {}
        public IOInfo(IODirection dir, int byteOffset, int bitOffset, String typeName) {
            this.dir = dir; this.byteOffset = byteOffset;
            this.bitOffset = bitOffset; this.typeName = typeName;
        }
    }

    public final Map<String, IOInfo> ioVarMap = new LinkedHashMap<>();

    // ─── POU 注册表 ───
    public final List<String> programNames = new ArrayList<>();
    public final Set<String> programNameSet = new HashSet<>();
    public String fileId = "";

    public void addProgramName(String name) {
        if (programNameSet.contains(name)) {
            throw new RuntimeException("Duplicate PROGRAM definition: " + name);
        }
        programNames.add(name);
        programNameSet.add(name);
    }

    public String mangleProgName(String progName) {
        return fileId.isEmpty() ? progName : fileId + "_" + progName;
    }

    public void setFileId(String id) {
        this.fileId = id;
    }

    public String getFileId() {
        return fileId.isEmpty() ? "unnamed" : fileId;
    }

    public List<String> getProgramNames() {
        return Collections.unmodifiableList(programNames);
    }

    // ─── FOR 循环 GVL 局部变量遮盖 ───
    public final Set<String> shadowedGvlVars = new HashSet<>();
    public final Set<String> locallyDeclaredGvlVars = new HashSet<>();
    public static class ShadowEntry {
        public String varName; public String type; public int offset;
        public ShadowEntry(String v, String t, int o) { varName = v; type = t; offset = o; }
    }
    public final ArrayDeque<ShadowEntry> shadowStack = new ArrayDeque<>();

    // ─── Struct/Field 类型（原 CodeGenerator 内） ───
    public static class StructField {
        public String name;
        public String type;
        public int offset;
        public StructField(String name, String type, int offset) {
            this.name = name; this.type = type; this.offset = offset;
        }
    }

    public static class StructLayout {
        public String structName;
        public List<StructField> fields;
        public int totalSize;
        public StructLayout(String structName, List<StructField> fields, int totalSize) {
            this.structName = structName;
            this.fields = fields;
            this.totalSize = totalSize;
        }
    }

    public void registerStructType(String typeName, String runtimeType, StructLayout layout) {
        structLayoutMap.put(typeName, layout);
        structTypeToName.put(runtimeType, typeName);
        SIZE_MAP.put(typeName, layout.totalSize);
    }

    public void registerEnumType(String enumName, String runtimeTypeName, String underlyingNativeType) {
        enumRuntimeToUnderlying.put(runtimeTypeName, underlyingNativeType);
        enumNameToUnderlying.put(enumName, underlyingNativeType);
    }

    public String emitEnumDecl(String enumName, String underlyingType, List<String> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nenum class ").append(enumName).append(" : ").append(underlyingType).append(" {");
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            sb.append(i == 0 ? "\n    " : ",\n    ").append(entry);
        }
        sb.append("\n};\n");
        return sb.toString();
    }

    public Integer getStructFieldOffset(String structTypeName, String fieldName) {
        StructLayout layout = structLayoutMap.get(structTypeName);
        if (layout == null) return null;
        for (StructField f : layout.fields) {
            if (f.name.equals(fieldName)) return f.offset;
        }
        return null;
    }

    public String getStructFieldType(String structTypeName, String fieldName) {
        StructLayout layout = structLayoutMap.get(structTypeName);
        if (layout == null) return null;
        for (StructField f : layout.fields) {
            if (f.name.equals(fieldName)) return f.type;
        }
        return null;
    }

    public Integer getVarOffset(String varName) {
        return offsetMap.get(varName);
    }

    public String getVarType(String varName) {
        return typeMap.get(varName);
    }

    // ST 类型名 → 原生 C++ 类型名
    public static final Map<String, String> TYPE_MAP = new HashMap<>();
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
    public static final Map<String, Integer> SIZE_MAP = new HashMap<>();
    private static void initSizeMap() {
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
    static { initSizeMap(); }

    public static void resetStatic() {
        SIZE_MAP.clear();
        initSizeMap();
    }

    public String toNativeType(String typeName) {
        if (typeName == null) return typeName;
        // 枚举类型保持原样，不降级为底层类型
        String mapped = structTypeToName.get(typeName);
        if (mapped != null) return mapped;
        mapped = TYPE_MAP.get(typeName);
        return mapped != null ? mapped : typeName;
    }

    public int getTypeSize(String nativeType) {
        if (nativeType == null) return 4;
        Integer size = SIZE_MAP.get(nativeType);
        if (size != null) return size;
        StructLayout layout = structLayoutMap.get(nativeType);
        if (layout != null) return layout.totalSize;
        String underlying = enumNameToUnderlying.get(nativeType);
        if (underlying != null) return getTypeSize(underlying);
        ArrayInfo arrayInfo = parseArrayType(nativeType);
        if (arrayInfo != null) {
            return arrayInfo.count * getTypeSize(arrayInfo.elemType);
        }
        return 4;
    }

    public int allocateOffset(String varName, String nativeType) {
        int size = getTypeSize(nativeType);
        int aligned = (currentOffset + size - 1) / size * size;
        offsetMap.put(varName, aligned);
        typeMap.put(varName, nativeType);
        currentOffset = aligned + size;
        return aligned;
    }

    public void registerVariable(String varName, String symbolId) {
        if (varName != null && symbolId != null) {
            symbolIdToNameMap.put(symbolId, varName);
            nameToSymbolIdMap.put(varName, symbolId);
        }
    }

    public String findVarNameBySymbolId(String symbolId) {
        return symbolIdToNameMap.get(symbolId);
    }

    // ═══ I/O 映射变量支持 ═══

    public IOInfo parseATAddress(String location) {
        if (location == null || !location.startsWith("%")) return null;
        IOInfo info = new IOInfo();
        int pos = 1;
        info.dir = IODirection.INPUT;
        if (pos < location.length()) {
            char c = location.charAt(pos);
            if (c == 'I' || c == 'Q' || c == 'M') {
                info.dir = (c == 'I') ? IODirection.INPUT
                        : (c == 'Q') ? IODirection.OUTPUT : IODirection.MEMORY;
                pos++;
            }
        }
        int sizeInBytes = 1;
        boolean isBit = false;
        if (pos < location.length()) {
            char c = location.charAt(pos);
            if (c == 'X') { isBit = true; sizeInBytes = 0; pos++; }
            else if (c == 'B') { pos++; }
            else if (c == 'W') { sizeInBytes = 2; pos++; }
            else if (c == 'D') { sizeInBytes = 4; pos++; }
            else if (c == 'L') { sizeInBytes = 8; pos++; }
        }
        StringBuilder numStr = new StringBuilder();
        while (pos < location.length() && Character.isDigit(location.charAt(pos))) {
            numStr.append(location.charAt(pos));
            pos++;
        }
        if (numStr.length() == 0) return null;
        int value = Integer.parseInt(numStr.toString());
        int bitOffset = -1;
        if (pos < location.length() && location.charAt(pos) == '.') {
            pos++;
            StringBuilder bitStr = new StringBuilder();
            while (pos < location.length() && Character.isDigit(location.charAt(pos))) {
                bitStr.append(location.charAt(pos));
                pos++;
            }
            if (bitStr.length() > 0) bitOffset = Integer.parseInt(bitStr.toString());
        }
        if (isBit) {
            info.byteOffset = value;
            info.bitOffset = (bitOffset >= 0) ? bitOffset : 0;
        } else {
            info.byteOffset = value * sizeInBytes;
            info.bitOffset = -1;
        }
        return info;
    }

    public void registerIOVariable(String varName, String typeName, String location) {
        if (varName == null || location == null) return;
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;
        IOInfo info = parseATAddress(location);
        if (info == null) {
            throw new RuntimeException("Invalid AT address: " + location + " for variable " + varName);
        }
        if (info.dir == IODirection.MEMORY) {
            return;
        }
        info.typeName = toNativeType(typeName);
        ioVarMap.put(cleanName, info);
    }

    public boolean isIOVariable(String varName) {
        if (varName == null) return false;
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;
        return ioVarMap.containsKey(cleanName);
    }

    public String emitIORead(String varName) {
        if (varName == null) return null;
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;
        IOInfo info = ioVarMap.get(cleanName);
        if (info == null) return null;
        if (info.dir == IODirection.MEMORY) return null;
        String prefix = (info.dir == IODirection.INPUT) ? "io.readInput" : "io.readOutput";
        if (info.bitOffset >= 0) {
            return prefix + "Bit(" + info.byteOffset + ", " + info.bitOffset + ")";
        }
        return prefix + "<" + info.typeName + ">(" + info.byteOffset + ")";
    }

    public String emitIOWrite(String varName, String valueExpr) {
        if (varName == null) return null;
        String cleanName = varName.startsWith("*") ? varName.substring(1) : varName;
        IOInfo info = ioVarMap.get(cleanName);
        if (info == null) return null;
        if (info.dir == IODirection.MEMORY) return null;
        if (info.bitOffset >= 0) {
            return "io.writeOutputBit(" + info.byteOffset + ", " + info.bitOffset + ", " + valueExpr + ")";
        }
        return "io.writeOutput<" + info.typeName + ">(" + info.byteOffset + ", " + valueExpr + ")";
    }

    // ═══ POU 注册表 ═══

    public String emitPOURegistration(String fileId, List<String> progNames) {
        if (progNames == null || progNames.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n// ─── Auto-generated POU Registration (").append(fileId).append(") ───\n");
        sb.append("void registerPOU_").append(fileId).append("(POURegistry& reg) {\n");
        for (String name : progNames) {
            String mangled = mangleProgName(name);
            sb.append("    POUCallbacks cbs;\n");
            sb.append("    cbs.init = PROGRAM_").append(mangled).append("_init;\n");
            sb.append("    cbs.cyclic = PROGRAM_").append(mangled).append("_cyclic;\n");
            sb.append("    cbs.pre = PROGRAM_").append(mangled).append("_pre;\n");
            sb.append("    cbs.post = PROGRAM_").append(mangled).append("_post;\n");
            sb.append("    reg.add(\"").append(name).append("\", cbs);\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    // ═══ FB 调用 ═══

    public String emitFBCall(String fbInstanceName, String fbTypeName,
                              List<String> paramNames, List<String> paramValues) {
        StringBuilder sb = new StringBuilder();
        Integer fbOffset = offsetMap.get(fbInstanceName);
        if (fbOffset == null) {
            sb.append("\n\t\t// FB instance ").append(fbInstanceName).append(" not in GVL, direct call");
            sb.append("\n\t\t").append(fbInstanceName).append(".update();");
            return sb.toString();
        }
        for (int i = 0; i < paramNames.size(); i++) {
            String fieldName = paramNames.get(i);
            String value = paramValues.get(i);
            Integer fieldOffset = getStructFieldOffset(fbTypeName, fieldName);
            if (fieldOffset != null) {
                String fieldType = getStructFieldType(fbTypeName, fieldName);
                sb.append("\n\t\tgvl.write<").append(fieldType).append(">(")
                  .append(fbOffset + fieldOffset).append(", ").append(value).append(");");
            }
        }
        sb.append("\n\t\tgvl.ptr<").append(fbTypeName).append(">(")
          .append(fbOffset).append(")->update();");
        return sb.toString();
    }

    // ═══ 数组类型信息 ═══

    public static class ArrayInfo {
        int count;
        String elemType;
    }

    public ArrayInfo parseArrayType(String typeName) {
        if (typeName == null) return null;
        ArrayInfo info = new ArrayInfo();
        Pattern rangePattern = Pattern.compile(
            "ARRAY\\[(\\d+)\\.\\.(\\d+)\\]\\s+OF\\s+(\\w+)");
        Matcher matcher = rangePattern.matcher(typeName);
        if (matcher.find()) {
            int low = Integer.parseInt(matcher.group(1));
            int high = Integer.parseInt(matcher.group(2));
            info.count = high - low + 1;
            info.elemType = toNativeType(matcher.group(3));
            return info;
        }
        Pattern simplePattern = Pattern.compile(
            "ARRAY\\[(\\d+)\\]\\s+OF\\s+(\\w+)");
        matcher = simplePattern.matcher(typeName);
        if (matcher.find()) {
            info.count = Integer.parseInt(matcher.group(1));
            info.elemType = toNativeType(matcher.group(2));
            return info;
        }
        return null;
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
