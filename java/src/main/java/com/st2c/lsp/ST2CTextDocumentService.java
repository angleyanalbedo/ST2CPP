package com.st2c.lsp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
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

    public void setWorkspaceFolders(List<org.eclipse.lsp4j.WorkspaceFolder> folders) {
        diagnostic.setWorkspaceFolders(folders);
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
