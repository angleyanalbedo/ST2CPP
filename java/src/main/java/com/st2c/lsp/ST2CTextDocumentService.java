package com.st2c.lsp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.st2c.lsp.analyzers.CompletionAnalyzer;
import com.st2c.lsp.analyzers.DiagnosticAnalyzer;
import com.st2c.lsp.analyzers.HoverAnalyzer;

public class ST2CTextDocumentService implements TextDocumentService {
    private final ST2CLanguageServer server;
    private final Map<String, String> documents = new ConcurrentHashMap<>();
    private final HoverAnalyzer hover = new HoverAnalyzer();
    private final CompletionAnalyzer completion = new CompletionAnalyzer();
    private final DiagnosticAnalyzer diagnostic = new DiagnosticAnalyzer();

    public ST2CTextDocumentService(ST2CLanguageServer server) {
        this.server = server;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String content = params.getTextDocument().getText();
        documents.put(uri, content);
        diagnostic.updateDocument(uri, content);
        publishAllDiagnostics();
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        var changes = params.getContentChanges();
        if (!changes.isEmpty()) {
            String content = changes.get(changes.size() - 1).getText();
            documents.put(uri, content);
            diagnostic.updateDocument(uri, content);
            publishAllDiagnostics();
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
        diagnostic.removeDocument(uri);
        publishAllDiagnostics();
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int col = params.getPosition().getCharacter();
        String content = documents.get(uri);
        return CompletableFuture.completedFuture(
            Either.forRight(completion.getCompletions(content, line, col))
        );
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int col = params.getPosition().getCharacter();
        String content = documents.get(uri);
        Hover result = hover.getHover(content, line, col);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int col = params.getPosition().getCharacter();
        String content = documents.get(uri);
        if (content == null) return CompletableFuture.completedFuture(null);

        String word = DiagnosticAnalyzer.extractWordAt(content, line, col);
        if (word == null) return CompletableFuture.completedFuture(Either.forLeft(List.of()));

        System.err.println("[LSP] definition request for word: '" + word + "' at line " + line + ", col " + col);
        var info = diagnostic.findDefinition(word);
        if (info == null) {
            System.err.println("[LSP] -> findDefinition returned null for word: " + word);
            return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        }

        System.err.println("[LSP] -> Found definition in " + info.uri + " at " + info.symbolLine + ":" + info.symbolColumn);
        Location loc = new Location(info.uri, new Range(
            new Position(info.symbolLine, info.symbolColumn),
            new Position(info.symbolLine, info.symbolColumn + word.length())
        ));
        return CompletableFuture.completedFuture(Either.forLeft(List.of(loc)));
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int col = params.getPosition().getCharacter();
        String content = documents.get(uri);
        if (content == null) return CompletableFuture.completedFuture(List.of());

        String word = DiagnosticAnalyzer.extractWordAt(content, line, col);
        if (word == null) return CompletableFuture.completedFuture(List.of());

        // 在所有打开的文件中搜索该符号的出现位置
        List<Location> results = new ArrayList<>();
        String lower = word.toLowerCase();

        // 只搜索已打开的文件
        for (var entry : diagnostic.getDocuments().entrySet()) {
            findOccurrences(entry.getValue(), entry.getKey(), lower, results);
        }

        return CompletableFuture.completedFuture(results);
    }

    private void findOccurrences(String content, String uri, String lowerWord, List<Location> results) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int idx = 0;
            while (true) {
                idx = line.toLowerCase().indexOf(lowerWord, idx);
                if (idx < 0) break;
                if (isWordBoundary(line, idx, lowerWord.length())) {
                    results.add(new Location(uri, new Range(
                        new Position(i, idx), new Position(i, idx + lowerWord.length())
                    )));
                }
                idx += lowerWord.length();
            }
        }
    }

    private boolean isWordBoundary(String line, int start, int len) {
        boolean leftOk = start == 0 || !Character.isJavaIdentifierPart(line.charAt(start - 1));
        int end = start + len;
        boolean rightOk = end >= line.length() || !Character.isJavaIdentifierPart(line.charAt(end));
        return leftOk && rightOk;
    }

    private void publishAllDiagnostics() {
        var client = server.getLanguageClient();
        if (client == null) return;
        for (String uri : documents.keySet()) {
            var params = new PublishDiagnosticsParams();
            params.setUri(uri);
            params.setDiagnostics(diagnostic.getDiagnostics(uri));
            client.publishDiagnostics(params);
        }
    }
}
