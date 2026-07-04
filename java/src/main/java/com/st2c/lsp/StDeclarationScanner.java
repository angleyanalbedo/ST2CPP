package com.st2c.lsp;

import antlr4.PLCSTPARSERBaseVisitor;
import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

/**
 * 轻量级 ST 声明扫描器：提取文件中定义的符号和引用的外部符号
 * 用于 LSP 的拓扑排序，确保文件按依赖顺序解析
 */
public class StDeclarationScanner {

    private static final Set<String> BASIC_TYPES = Set.of(
        "BOOL", "SINT", "INT", "DINT", "LINT", "REAL", "LREAL",
        "STRING", "WSTRING", "CHAR", "WCHAR",
        "TIME", "DATE", "TOD", "DT",
        "BYTE", "WORD", "DWORD", "LWORD",
        "USINT", "UINT", "UDINT", "ULINT",
        "ARRAY", "REF", "POINTER",
        "TRUE", "FALSE"
    );

    public static class SymbolPos {
        public final int line; // 1-based
        public final int col;  // 0-based
        public SymbolPos(int line, int col) { this.line = line; this.col = col; }
    }

    /** 一个文件的声明/引用信息 */
    public static class FileInfo {
        public final String path;
        public final Map<String, SymbolPos> defines = new LinkedHashMap<>();
        public final Set<String> references = new HashSet<>();

        public FileInfo(String path) { this.path = path; }
    }

    /**
     * 拓扑排序的结果
     */
    public static class SortResult {
        public final List<String> sortedFiles;
        /** 符号名(小写) → 定义它的文件路径 */
        public final Map<String, String> symbolToFile;
        /** 符号名(小写) → 定义它的位置 */
        public final Map<String, SymbolPos> symbolToPos;
        SortResult(List<String> sortedFiles, Map<String, String> symbolToFile, Map<String, SymbolPos> symbolToPos) {
            this.sortedFiles = sortedFiles;
            this.symbolToFile = symbolToFile;
            this.symbolToPos = symbolToPos;
        }
    }

    /**
     * 扫描多个文件，按拓扑排序返回处理顺序
     */
    public static SortResult topologicalSortWithDetails(
            List<String> filePaths,
            java.util.function.Function<String, String> contentProvider) {

        Map<String, FileInfo> infoMap = new LinkedHashMap<>();
        for (String path : filePaths) {
            String content = contentProvider.apply(path);
            if (content != null) {
                infoMap.put(path, scan(content, path));
            }
        }

        Map<String, String> symbolToFile = new HashMap<>();
        Map<String, SymbolPos> symbolToPos = new HashMap<>();
        for (FileInfo info : infoMap.values()) {
            for (var entry : info.defines.entrySet()) {
                String sym = entry.getKey().toLowerCase();
                symbolToFile.putIfAbsent(sym, info.path);
                symbolToPos.putIfAbsent(sym, entry.getValue());
            }
        }

        Map<String, Set<String>> deps = new HashMap<>();
        for (FileInfo info : infoMap.values()) {
            Set<String> fileDeps = new HashSet<>();
            for (String ref : info.references) {
                String definingFile = symbolToFile.get(ref.toLowerCase());
                if (definingFile != null && !definingFile.equals(info.path)) {
                    fileDeps.add(definingFile);
                }
            }
            deps.put(info.path, fileDeps);
        }

        List<String> sorted = kahnSort(filePaths, deps);
        return new SortResult(sorted, symbolToFile, symbolToPos);
    }

    /**
     * 兼容旧接口：只返回排序结果
     */
    public static List<String> topologicalSort(
            List<String> filePaths,
            java.util.function.Function<String, String> contentProvider) {
        return topologicalSortWithDetails(filePaths, contentProvider).sortedFiles;
    }

    private static List<String> kahnSort(
            List<String> allFiles,
            Map<String, Set<String>> deps) {

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> reverseDeps = new HashMap<>();

        for (String f : allFiles) {
            inDegree.putIfAbsent(f, 0);
            reverseDeps.putIfAbsent(f, new ArrayList<>());
        }

        for (var entry : deps.entrySet()) {
            String file = entry.getKey();
            for (String dep : entry.getValue()) {
                if (inDegree.containsKey(dep)) {
                    inDegree.merge(file, 1, Integer::sum);
                    reverseDeps.computeIfAbsent(dep, k -> new ArrayList<>()).add(file);
                }
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            result.add(node);
            for (String dependent : reverseDeps.getOrDefault(node, List.of())) {
                int newDegree = inDegree.merge(dependent, -1, Integer::sum);
                if (newDegree == 0) queue.add(dependent);
            }
        }

        Set<String> sorted = new HashSet<>(result);
        for (String f : allFiles) {
            if (!sorted.contains(f)) {
                result.add(f);
            }
        }

        return result;
    }

    static FileInfo scan(String content, String path) {
        FileInfo info = new FileInfo(path);
        try {
            PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(CharStreams.fromString(content));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
            parser.removeErrorListeners();
            ParseTree tree = parser.startpoint();
            new DeclarationVisitor(info).visit(tree);
        } catch (Exception ignored) {}
        return info;
    }

    private static class DeclarationVisitor extends PLCSTPARSERBaseVisitor<Void> {
        private final FileInfo info;
        DeclarationVisitor(FileInfo info) { this.info = info; }

        @Override
        public Void visitStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx) {
            if (ctx.struct_type_name() != null)
                info.defines.put(ctx.struct_type_name().getText(), new SymbolPos(ctx.struct_type_name().start.getLine(), ctx.struct_type_name().start.getCharPositionInLine()));
            return super.visitStruct_type_decl(ctx);
        }

        @Override
        public Void visitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx) {
            if (ctx.enum_type_name() != null)
                info.defines.put(ctx.enum_type_name().getText(), new SymbolPos(ctx.enum_type_name().start.getLine(), ctx.enum_type_name().start.getCharPositionInLine()));
            return super.visitEnum_type_decl(ctx);
        }

        @Override
        public Void visitFb_decl(PLCSTPARSERParser.Fb_declContext ctx) {
            if (ctx.derived_fb_name() != null)
                info.defines.put(ctx.derived_fb_name().getText(), new SymbolPos(ctx.derived_fb_name().start.getLine(), ctx.derived_fb_name().start.getCharPositionInLine()));
            return super.visitFb_decl(ctx);
        }

        @Override
        public Void visitFunc_decl(PLCSTPARSERParser.Func_declContext ctx) {
            if (ctx.derived_func_name() != null)
                info.defines.put(ctx.derived_func_name().getText(), new SymbolPos(ctx.derived_func_name().start.getLine(), ctx.derived_func_name().start.getCharPositionInLine()));
            return super.visitFunc_decl(ctx);
        }

        @Override
        public Void visitProg_decl(PLCSTPARSERParser.Prog_declContext ctx) {
            if (ctx.prog_type_name() != null)
                info.defines.put(ctx.prog_type_name().getText(), new SymbolPos(ctx.prog_type_name().start.getLine(), ctx.prog_type_name().start.getCharPositionInLine()));
            return super.visitProg_decl(ctx);
        }

        @Override
        public Void visitGlobal_var_spec(PLCSTPARSERParser.Global_var_specContext ctx) {
            for (var nameCtx : ctx.global_var_name())
                info.defines.put(nameCtx.getText(), new SymbolPos(nameCtx.start.getLine(), nameCtx.start.getCharPositionInLine()));
            return super.visitGlobal_var_spec(ctx);
        }

        @Override
        public Void visitStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx) {
            if (ctx.struct_elem_name() != null)
                info.defines.put(ctx.struct_elem_name().getText(), new SymbolPos(ctx.struct_elem_name().start.getLine(), ctx.struct_elem_name().start.getCharPositionInLine()));
            return super.visitStruct_elem_decl(ctx);
        }

        @Override
        public Void visitVariable_name(PLCSTPARSERParser.Variable_nameContext ctx) {
            info.defines.put(ctx.getText(), new SymbolPos(ctx.start.getLine(), ctx.start.getCharPositionInLine()));
            return super.visitVariable_name(ctx);
        }

        @Override
        public Void visitEnum_value_spec(PLCSTPARSERParser.Enum_value_specContext ctx) {
            if (ctx.identifier() != null) {
                info.defines.put(ctx.identifier().getText(), new SymbolPos(ctx.identifier().start.getLine(), ctx.identifier().start.getCharPositionInLine()));
            }
            return super.visitEnum_value_spec(ctx);
        }

        @Override
        public Void visitUser_defination_type_access(
                PLCSTPARSERParser.User_defination_type_accessContext ctx) {
            if (ctx.user_defination_type_name() != null)
                info.references.add(ctx.user_defination_type_name().getText());
            return super.visitUser_defination_type_access(ctx);
        }

        @Override
        public Void visitSimple_type_access(
                PLCSTPARSERParser.Simple_type_accessContext ctx) {
            // 独立扫描时没有符号表，用户类型会被 ANTLR 归为 simple_type_access
            // 所以要从这里收集非基本类型的引用
            if (ctx.simple_type_name() != null) {
                String name = ctx.simple_type_name().getText();
                if (!BASIC_TYPES.contains(name.toUpperCase())) {
                    info.references.add(name);
                }
            }
            return super.visitSimple_type_access(ctx);
        }

        @Override
        public Void visitFunc_call(PLCSTPARSERParser.Func_callContext ctx) {
            if (ctx.func_access() != null
                    && ctx.func_access().func_name() != null
                    && ctx.func_access().func_name().derived_func_name() != null) {
                info.references.add(ctx.func_access().func_name().derived_func_name().getText());
            }
            return super.visitFunc_call(ctx);
        }
    }
}
