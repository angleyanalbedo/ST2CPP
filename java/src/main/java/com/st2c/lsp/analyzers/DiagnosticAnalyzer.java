package com.st2c.lsp.analyzers;

import java.io.IOException;
import java.nio.file.FileSystems;
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
    private final Map<String, List<Diagnostic>> diagnosticsMap = new HashMap<>();
    private String workspaceRoot;
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

    public synchronized void setWorkspaceRoot(String root) {
        workspaceRoot = root;
    }

    public synchronized void setWorkspaceFolders(List<org.eclipse.lsp4j.WorkspaceFolder> folders) {
        if (folders != null && !folders.isEmpty()) {
            workspaceRoot = uriToPath(folders.get(0).getUri());
        }
    }

    public void updateDocument(String uri, String content) {
        synchronized (this) {
            documents.put(uri, content);
            // 初次打开文件时用文件目录作为 workspaceRoot
            if (workspaceRoot == null) {
                workspaceRoot = uriToPath(uri);
                if (workspaceRoot != null) {
                    workspaceRoot = Paths.get(workspaceRoot).getParent().toString();
                }
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

    private void reanalyzeAll() {
        ensureStrategies();
        synchronized (this) {
            diagnosticsMap.clear();
            if (documents.isEmpty()) return;

            // 重置全局编译器状态
            CompilerState.reset();

            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);

            // 1. 先加载同目录下的其他 .st 文件（仅构建符号表，不产生诊断）
            List<String> supportFiles = scanStFiles(workspaceRoot);
            for (String filePath : supportFiles) {
                String fileUri = pathToUri(filePath);
                if (documents.containsKey(fileUri)) continue;
                try {
                    String content = Files.readString(Paths.get(filePath));
                    CharStream cs = CharStreams.fromString(content);
                    PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(cs);
                    CommonTokenStream tokens = new CommonTokenStream(lexer);
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
                    visitor.visit(parser.startpoint());
                } catch (Exception ignored) {
                    // 支持文件分析失败不影响诊断
                }
            }

            // 2. 正式分析所有已打开的文件
            for (var entry : documents.entrySet()) {
                String uri = entry.getKey();
                String content = entry.getValue();
                List<Diagnostic> fileDiags = new ArrayList<>();
                try {
                    CharStream charStream = CharStreams.fromString(content);
                    PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(charStream);
                    CommonTokenStream tokens = new CommonTokenStream(lexer);
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
                    visitor.visit(parser.startpoint());
                } catch (Exception e) {
                    Diagnostic d = createDiagnostic(e);
                    if (d != null) fileDiags.add(d);
                }
                diagnosticsMap.put(uri, fileDiags);
            }
        }
    }

    private List<String> scanStFiles(String dir) {
        if (dir == null) return List.of();
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                .filter(p -> p.toString().endsWith(".st"))
                .map(Path::toString)
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String uriToPath(String uri) {
        if (uri == null) return null;
        if (uri.startsWith("file:///")) {
            return uri.substring(8).replace('/', '\\');
        }
        if (uri.startsWith("file:/")) {
            return uri.substring(6).replace('/', '\\');
        }
        return uri;
    }

    private static String pathToUri(String path) {
        return "file:///" + path.replace('\\', '/');
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

        Range range = new Range(start, end);
        Diagnostic diagnostic = new Diagnostic(range, message);
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        return diagnostic;
    }
}
