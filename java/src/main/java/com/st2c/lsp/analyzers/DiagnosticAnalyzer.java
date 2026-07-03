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
            if (documents.isEmpty()) return;

            CompilerState.reset();
            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);

            // 解析支持文件（仅建符号表，不产生诊断）
            List<String> ordered = supportFiles.stream()
                .sorted((a, b) -> {
                    String fa = Paths.get(a).getFileName().toString().toLowerCase();
                    String fb = Paths.get(b).getFileName().toString().toLowerCase();
                    boolean aIsType = fa.startsWith("type") || fa.startsWith("io_config");
                    boolean bIsType = fb.startsWith("type") || fb.startsWith("io_config");
                    if (aIsType && !bIsType) return -1;
                    if (!aIsType && bIsType) return 1;
                    return fa.compareTo(fb);
                })
                .collect(Collectors.toList());

            // 第一步：解析所有支持文件（不管是否也在已打开列表中），建立完整符号表
            for (String path : ordered) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(path)));
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(
                        new CommonTokenStream(new PLCSTPARSERLexer(CharStreams.fromString(content))));
                    visitor.visit(parser.startpoint());
                } catch (Exception ignored) {}
            }

            // 第二步：分析已打开的文件（共享符号表，跨文件类型可解析）
            for (var entry : documents.entrySet()) {
                List<Diagnostic> diags = new ArrayList<>();
                try {
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(
                        new CommonTokenStream(new PLCSTPARSERLexer(CharStreams.fromString(entry.getValue()))));
                    visitor.visit(parser.startpoint());
                } catch (Exception e) {
                    Diagnostic d = createDiagnostic(e);
                    if (d != null) diags.add(d);
                }
                diagnosticsMap.put(entry.getKey(), diags);
            }
        }
    }

    private String getDirFromUri(String uri) {
        try {
            // VS Code 发送的 URI 是 URL 编码的（d%3A → d:），需要解码
            String decoded = java.net.URLDecoder.decode(uri, "UTF-8");
            String path = decoded.startsWith("file:///") ? decoded.substring(8).replace('/', '\\')
                     : decoded.startsWith("file:/") ? decoded.substring(6).replace('/', '\\')
                     : decoded;
            Path parent = Paths.get(path).getParent();
            return parent != null ? parent.toString() : "";
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
