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
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import PLCException.PLCSemanticException;
import PLCSymbolAndScope.CompilerState;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCTypeDeclSymbol;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;

import com.st2c.lsp.StDeclarationScanner;

public class DiagnosticAnalyzer {
    private final Map<String, String> documents = new LinkedHashMap<>();
    private final List<String> supportFiles = new ArrayList<>();
    private final Map<String, List<Diagnostic>> diagnosticsMap = new LinkedHashMap<>();
    private boolean strategiesRegistered = false;

    // 符号定义索引: symbolName_lowercase → (URI, line, col)
    private final Map<String, DefinitionInfo> symbolDefinitions = new LinkedHashMap<>();

    public static class DefinitionInfo {
        public final String uri;
        public final int line;    // 1-based
        public final int column;  // 1-based
        public final int symbolLine; // 0-based for ANTLR
        public final int symbolColumn;
        public DefinitionInfo(String uri, int line, int col) {
            this.uri = uri; this.line = line; this.column = col;
            this.symbolLine = line - 1; this.symbolColumn = col;
        }
    }

    public DefinitionInfo findDefinition(String name) {
        return symbolDefinitions.get(name.toLowerCase());
    }

    public Map<String, String> getDocuments() {
        return documents;
    }

    public List<String> getSupportFiles() {
        return supportFiles;
    }

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
            symbolDefinitions.clear();
            if (documents.isEmpty() || supportFiles.isEmpty()) return;

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

            var sortResult = StDeclarationScanner.topologicalSortWithDetails(
                new ArrayList<>(supportFiles), contentProvider);
            List<String> ordered = sortResult.sortedFiles;
            Map<String, String> symbolToFile = sortResult.symbolToFile;

            CompilerState.reset();
            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);

            Map<String, String> pathToUri = new LinkedHashMap<>();
            // 已打开的文档
            for (String uri : documents.keySet()) {
                String p = getPathFromUri(uri);
                if (!p.isEmpty()) pathToUri.put(p, uri);
            }
            // 未打开的支持文件（用于符号定义指向）
            for (String sp : supportFiles) {
                pathToUri.putIfAbsent(sp, pathToUri(sp));
            }

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

                String uri = pathToUri.get(path);
                if (uri != null) {
                    diagnosticsMap.put(uri, diags);
                }
            }

            for (String key : symbolToFile.keySet()) {
                String filePath = symbolToFile.get(key);
                if (filePath != null) {
                    String uri = pathToUri.get(filePath);
                    if (uri != null) {
                        var pos = sortResult.symbolToPos.get(key);
                        if (pos != null) {
                            symbolDefinitions.put(key, new DefinitionInfo(uri, pos.line, pos.col));
                            System.err.println("[LSP] Indexed symbol definition: " + key + " -> " + uri + " (" + pos.line + ":" + pos.col + ")");
                        } else {
                            System.err.println("[LSP] Warning: pos not found for symbol: " + key);
                        }
                    } else {
                        System.err.println("[LSP] Warning: URI not found for filePath: " + filePath + " (symbol: " + key + ")");
                    }
                } else {
                    System.err.println("[LSP] Warning: symbolToFile missing filePath for symbol: " + key);
                }
            }

        }
    }

    public static String extractWordAt(String content, int line, int col) {
        String[] lines = content.split("\n", -1);
        if (line < 0 || line >= lines.length) return null;
        String l = lines[line];
        if (col < 0 || col >= l.length()) return null;

        int start = col;
        while (start > 0 && (Character.isJavaIdentifierPart(l.charAt(start - 1)) || l.charAt(start - 1) == '_'))
            start--;
        int end = col;
        while (end < l.length() && (Character.isJavaIdentifierPart(l.charAt(end)) || l.charAt(end) == '_'))
            end++;

        if (start == end) return null;
        return l.substring(start, end);
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

    public String getPathFromUri(String uri) {
        try {
            String decoded = java.net.URLDecoder.decode(uri, "UTF-8");
            String path = decoded.startsWith("file:///") ? decoded.substring(8).replace('/', '\\')
                     : decoded.startsWith("file:/") ? decoded.substring(6).replace('/', '\\')
                     : decoded;
            return Paths.get(path).normalize().toString();
        } catch (Exception e) { return ""; }
    }

    /** 文件系统路径 → file:// URI */
    private static String pathToUri(String filePath) {
        try {
            return Paths.get(filePath).toUri().toString();
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
