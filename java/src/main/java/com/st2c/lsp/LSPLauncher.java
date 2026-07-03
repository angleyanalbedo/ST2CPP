package com.st2c.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

public class LSPLauncher {
    public static void launch(ST2CLanguageServer server) {
        InputStream in = System.in;
        OutputStream out = System.out;

        Launcher<LanguageClient> launcher = Launcher.createLauncher(
            server,
            LanguageClient.class,
            in,
            out
        );

        LanguageClient client = launcher.getRemoteProxy();
        server.setLanguageClient(client);

        Future<Void> startListening = launcher.startListening();

        try {
            startListening.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
