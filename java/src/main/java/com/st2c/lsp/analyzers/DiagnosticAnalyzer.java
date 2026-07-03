package com.st2c.lsp.analyzers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, String> documents = new HashMap<>();
    private final Map<String, String> supportFiles = new HashMap<>();
    private final Map<String, List<Diagnostic>> diagnosticsMap = new HashMap<>();
    private boolean strategiesRegistered = false;

    private synchronized void ensureStrategies() {
        if (strategiesRegistered) return;
        try {
            new Registrant().autoRegister();
            strategiesRegistered = true;
        } catch (Exception e) {
        }
    }

    public void updateDocument(String uri, String content) {
        synchronized (this) {
            documents.put(uri, content);
            if (supportFiles.isEmpty()) {
                loadSupportFiles(getDirFromUri(uri));
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
        if (!Files.isDirectory(dirPath)) return;
        try (var stream = Files.list(dirPath)) {
            stream.filter(p -> p.toString().endsWith(".st"))
                  .forEach(p -> {
                      supportFiles.put(p.toString(), "");
                      System.err.println("[LSP] Support file registered: " + p.getFileName());
                  });
            System.err.println("[LSP] Loaded " + supportFiles.size() + " support files from " + dir);
        } catch (IOException e) {
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

            // 自动加载同目录 .st 支持文件（仅构建符号表，不产生诊断）
            System.err.println("[LSP] Support files to parse: " + supportFiles.size());
            for (String path : supportFiles.keySet()) {
                String uri = "file:///" + path.replace('\\', '/');
                if (documents.containsKey(uri)) continue;
                try {
                    String content = new String(Files.readAllBytes(Paths.get(path)));
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(
                        new CommonTokenStream(new PLCSTPARSERLexer(CharStreams.fromString(content))));
                    System.err.println("[LSP] Parsing support file: " + Paths.get(path).getFileName());
                    visitor.visit(parser.startpoint());
                    System.err.println("[LSP] OK: " + Paths.get(path).getFileName());
                } catch (Exception e) {
                    System.err.println("[LSP] SKIP (error): " + Paths.get(path).getFileName() + " - " + e.getMessage());
                }
            }

            // 分析已打开的文件（共享符号表，跨文件类型引用可解析）
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
            String path;
            if (uri.startsWith("file:///"))
                path = uri.substring(8).replace('/', '\\');
            else if (uri.startsWith("file:/"))
                path = uri.substring(6).replace('/', '\\');
            else
                path = uri;
            Path parent = Paths.get(path).getParent();
            return parent != null ? parent.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Diagnostic createDiagnostic(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) return null;
        Position start = new Position(0, 0), end = new Position(0, 0);
        if (e instanceof org.antlr.v4.runtime.RecognitionException) {
            var re = (org.antlr.v4.runtime.RecognitionException) e;
            start = new Position(re.getOffendingToken().getLine() - 1,
                re.getOffendingToken().getCharPositionInLine());
            end = new Position(start.getLine(),
                start.getCharacter() + re.getOffendingToken().getText().length());
        } else if (e instanceof PLCSemanticException) {
            var ctx = ((PLCSemanticException) e).getCtx();
            if (ctx != null) {
                start = new Position(ctx.getStart().getLine() - 1,
                    ctx.getStart().getCharPositionInLine());
                end = new Position(ctx.getStop().getLine() - 1,
                    ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length());
            }
        }
        var d = new Diagnostic(new Range(start, end), msg);
        d.setSeverity(DiagnosticSeverity.Error);
        return d;
    }
}
