package com.st2c.lsp.analyzers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
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
            System.err.println("[DiagnosticAnalyzer] Strategy registration failed: " + e.getMessage());
        }
    }

    public void updateDocument(String uri, String content) {
        synchronized (this) {
            documents.put(uri, content);
            // 首次打开文件时自动加载同目录 .st 支持文件
            if (supportFiles.isEmpty()) {
                loadSupportFiles(getDirFromUri(uri));
            }
        }
        reanalyzeAll();
    }

    public void removeDocument(String uri) {
        synchronized (this) {
            documents.remove(uri);
        }
        reanalyzeAll();
    }

    public List<Diagnostic> getDiagnostics(String uri) {
        synchronized (this) {
            return diagnosticsMap.getOrDefault(uri, List.of());
        }
    }

    private void loadSupportFiles(String dir) {
        if (dir.isEmpty()) return;
        Path dirPath = Paths.get(dir);
        if (!Files.isDirectory(dirPath)) return;
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.filter(p -> p.toString().endsWith(".st"))
                  .forEach(p -> supportFiles.put(p.toString(), ""));
        } catch (IOException e) {
        }
        // 延迟加载内容（首次 reanalyzeAll 时读）
    }

    private void reanalyzeAll() {
        ensureStrategies();
        synchronized (this) {
            diagnosticsMap.clear();
            if (documents.isEmpty()) return;

            // 重置全局编译器状态
            CompilerState.reset();
            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);

            // 第一步：解析支持文件（仅建符号表）
            for (String path : supportFiles.keySet()) {
                String uri = "file:///" + path.replace('\\', '/');
                if (documents.containsKey(uri)) continue;
                try {
                    byte[] raw = Files.readAllBytes(Paths.get(path));
                    parseOnce(visitor, new String(raw));
                } catch (Exception e) {
                    // 支持文件解析失败不影响主文件
                }
            }

            // 第二步：解析已打开的文件
            for (var entry : documents.entrySet()) {
                List<Diagnostic> diags = new ArrayList<>();
                try {
                    parseOnce(visitor, entry.getValue());
                } catch (Exception e) {
                    Diagnostic d = createDiagnostic(e);
                    if (d != null) diags.add(d);
                }
                diagnosticsMap.put(entry.getKey(), diags);
            }
        }
    }

    private void parseOnce(PLCVisitor visitor, String source) {
        PLCSTPARSERParser parser = new PLCSTPARSERParser(
            new CommonTokenStream(new PLCSTPARSERLexer(CharStreams.fromString(source))));
        visitor.visit(parser.startpoint());
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
        String message = e.getMessage();
        if (message == null || message.isEmpty()) return null;
        Position start = new Position(0, 0);
        Position end = new Position(0, 0);
        if (e instanceof org.antlr.v4.runtime.RecognitionException) {
            org.antlr.v4.runtime.RecognitionException re = (org.antlr.v4.runtime.RecognitionException) e;
            start = new Position(re.getOffendingToken().getLine() - 1,
                re.getOffendingToken().getCharPositionInLine());
            end = new Position(start.getLine(),
                start.getCharacter() + re.getOffendingToken().getText().length());
        } else if (e instanceof PLCSemanticException) {
            PLCSemanticException se = (PLCSemanticException) e;
            if (se.getCtx() != null) {
                start = new Position(se.getCtx().getStart().getLine() - 1,
                    se.getCtx().getStart().getCharPositionInLine());
                end = new Position(se.getCtx().getStop().getLine() - 1,
                    se.getCtx().getStop().getCharPositionInLine() + se.getCtx().getStop().getText().length());
            }
        }
        Diagnostic diagnostic = new Diagnostic(new Range(start, end), message);
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        return diagnostic;
    }
}
