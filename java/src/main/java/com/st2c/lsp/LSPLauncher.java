package com.st2c.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;

public class LSPLauncher {
    public static void launch(LanguageServer server) {
        InputStream in = System.in;
        OutputStream out = System.out;

        Launcher<LanguageServer> launcher = Launcher.createLauncher(
            server,
            LanguageServer.class,
            in,
            out
        );

        Future<Void> startListening = launcher.startListening();

        try {
            startListening.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
