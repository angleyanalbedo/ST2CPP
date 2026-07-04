package com.st2c.lsp.analyzers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import PLCException.PLCSemanticException;
import PLCSymbolAndScope.CompilerState;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;

import com.st2c.lsp.StDeclarationScanner;

public class DiagnosticAnalyzer {
    private final Map<String, String> documents = new LinkedHashMap<>();
    private final List<String> supportFiles = new ArrayList<>();
    private final Map<String, List<Diagnostic>> diagnosticsMap = new LinkedHashMap<>();
    private boolean strategiesRegistered = false;

    private synchronized void ensureStrategies() {
        if (strategiesRegistered) return;
        try { new Registrant().autoRegister(); strategiesRegistered = true; } catch (Exception e) {}
    }

    public void updateDocument(String uri, String content) {
        synchronized (this) {
            boolean isNew = !documents.containsKey(uri);
            documents.put(uri, content);
            if (isNew && supportFiles.isEmpty()) {
                String dir = getDirFromUri(uri);
                System.err.println("[LSP] Opening: " + uri + " -> dir: " + dir);
                loadSupportFiles(dir);
            }
        }
        reanalyzeAll();
    }

    public void removeDocument(String uri) {
        synchronized (this) { documents.remove(uri); }
        reanalyzeAll();
    }

    public List<Diagnostic> getDiagnostics(String uri) {
        synchronized (this) { return diagnosticsMap.getOrDefault(uri, List.of()); }
    }

    private void loadSupportFiles(String dir) {
        if (dir.isEmpty()) return;
        Path dirPath = Paths.get(dir);
        if (!Files.isDirectory(dirPath)) {
            System.err.println("[LSP] Directory not found: " + dir);
            return;
        }
        try (Stream<Path> stream = Files.list(dirPath)) {
            List<String> files = stream
                .filter(p -> p.toString().endsWith(".st"))
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
            supportFiles.addAll(files);
            System.err.println("[LSP] Loaded " + files.size() + " support files from " + dir);
        } catch (IOException e) {
            System.err.println("[LSP] Failed to scan: " + dir + " - " + e.getMessage());
        }
    }

    private void reanalyzeAll() {
        ensureStrategies();
        synchronized (this) {
            diagnosticsMap.clear();
            if (documents.isEmpty() || supportFiles.isEmpty()) return;

            // 构建内容提供者：打开文档用内存内容，其余读磁盘
            java.util.function.Function<String, String> contentProvider = path -> {
                for (var entry : documents.entrySet()) {
                    String docPath = getPathFromUri(entry.getKey());
                    if (docPath.equals(path)) {
                        return entry.getValue();
                    }
                }
                try {
                    return new String(Files.readAllBytes(Paths.get(path)));
                } catch (IOException e) {
                    return null;
                }
            };

            // 拓扑排序所有支持文件
            List<String> ordered = StDeclarationScanner.topologicalSort(
                new ArrayList<>(supportFiles), contentProvider);

            // 单趟全量分析，共享 PLCVisitor
            CompilerState.reset();
            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);

            // 预计算 URI ↔ 路径映射
            Map<String, String> pathToUri = new LinkedHashMap<>();
            for (String uri : documents.keySet()) {
                String p = getPathFromUri(uri);
                if (!p.isEmpty()) pathToUri.put(p, uri);
            }

            // 单趟：按拓扑序解析每个文件一次
            for (String path : ordered) {
                String content = contentProvider.apply(path);
                if (content == null) continue;

                List<Diagnostic> diags = new ArrayList<>();
                try {
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(
                        new CommonTokenStream(new PLCSTPARSERLexer(CharStreams.fromString(content))));
                    visitor.visit(parser.startpoint());
                } catch (Exception e) {
                    Diagnostic d = createDiagnostic(e);
                    if (d != null) diags.add(d);
                }

                // 如果是打开文档，保存诊断
                String uri = pathToUri.get(path);
                if (uri != null) {
                    diagnosticsMap.put(uri, diags);
                }
            }
        }
    }

    private String getDirFromUri(String uri) {
        try {
            String decoded = java.net.URLDecoder.decode(uri, "UTF-8");
            String path = decoded.startsWith("file:///") ? decoded.substring(8).replace('/', '\\')
                     : decoded.startsWith("file:/") ? decoded.substring(6).replace('/', '\\')
                     : decoded;
            Path parent = Paths.get(path).getParent();
            return parent != null ? parent.toString() : "";
        } catch (Exception e) { return ""; }
    }

    private String getPathFromUri(String uri) {
        try {
            String decoded = java.net.URLDecoder.decode(uri, "UTF-8");
            String path = decoded.startsWith("file:///") ? decoded.substring(8).replace('/', '\\')
                     : decoded.startsWith("file:/") ? decoded.substring(6).replace('/', '\\')
                     : decoded;
            return Paths.get(path).normalize().toString();
        } catch (Exception e) { return ""; }
    }

    private Diagnostic createDiagnostic(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) return null;
        Position start = new Position(0, 0), end = new Position(0, 0);
        if (e instanceof org.antlr.v4.runtime.RecognitionException) {
            var re = (org.antlr.v4.runtime.RecognitionException) e;
            start = new Position(re.getOffendingToken().getLine() - 1, re.getOffendingToken().getCharPositionInLine());
            end = new Position(start.getLine(), start.getCharacter() + re.getOffendingToken().getText().length());
        } else if (e instanceof PLCSemanticException) {
            var ctx = ((PLCSemanticException) e).getCtx();
            if (ctx != null) {
                start = new Position(ctx.getStart().getLine() - 1, ctx.getStart().getCharPositionInLine());
                end = new Position(ctx.getStop().getLine() - 1,
                    ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length());
            }
        }
        var d = new Diagnostic(new Range(start, end), msg);
        d.setSeverity(DiagnosticSeverity.Error);
        return d;
    }
}
