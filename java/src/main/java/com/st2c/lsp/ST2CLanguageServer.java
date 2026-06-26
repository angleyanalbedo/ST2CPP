package com.st2c.lsp;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class ST2CLanguageServer implements LanguageServer {
    private final TextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    private int processId;

    public ST2CLanguageServer() {
        this.textDocumentService = new ST2CTextDocumentService(this);
        this.workspaceService = new ST2CWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        if (params != null) {
            this.processId = params.getProcessId();
        }

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        CompletionOptions completionOptions = new CompletionOptions();
        capabilities.setCompletionProvider(completionOptions);

        capabilities.setHoverProvider(true);
        capabilities.setDefinitionProvider(true);

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

    public static void main(String[] args) {
        ST2CLanguageServer server = new ST2CLanguageServer();
        LSPLauncher.launch(server);
    }
}
