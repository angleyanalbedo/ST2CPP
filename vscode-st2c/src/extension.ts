import * as path from 'path';
import { ExtensionContext } from 'vscode';
import { LanguageClient, TransportKind, ServerOptions, LanguageClientOptions } from 'vscode-languageclient/node';

let client: LanguageClient;

export function activate(context: ExtensionContext) {
    const serverModule = context.asAbsolutePath(
        path.join('..', 'java', 'target', 'st2c-lsp-server-jar-with-dependencies.jar')
    );

    const serverOptions: ServerOptions = {
        run: {
            command: 'java',
            args: ['-jar', serverModule],
            transport: TransportKind.stdio
        },
        debug: {
            command: 'java',
            args: ['-jar', serverModule],
            transport: TransportKind.stdio
        }
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'st' }],
        synchronize: {
            fileEvents: context.workspace.createFileSystemWatcher('**/*.st')
        }
    };

    client = new LanguageClient(
        'st2c',
        'ST2C Language Server',
        serverOptions,
        clientOptions
    );

    client.start();
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}
