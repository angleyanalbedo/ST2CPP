package com.st2c.lsp;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class ST2CLanguageServer implements LanguageServer {
    private final ST2CTextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    private LanguageClient client;
    private int processId;

    public ST2CLanguageServer() {
        this.textDocumentService = new ST2CTextDocumentService(this);
        this.workspaceService = new ST2CWorkspaceService();
    }

    public void setLanguageClient(LanguageClient client) {
        this.client = client;
    }

    public LanguageClient getLanguageClient() {
        return client;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        if (params != null) {
            this.processId = params.getProcessId();
        }

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        capabilities.setCompletionProvider(new CompletionOptions());
        capabilities.setHoverProvider(true);
        capabilities.setDefinitionProvider(true);
        capabilities.setReferencesProvider(true);

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    public int getProcessId() {
        return processId;
    }

    @Override
    public void setTrace(SetTraceParams params) {
        // VS Code 会发 $/setTrace，静默忽略
    }

    public static void main(String[] args) {
        ST2CLanguageServer server = new ST2CLanguageServer();
        LSPLauncher.launch(server);
    }
}
