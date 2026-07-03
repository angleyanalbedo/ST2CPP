const path = require('path');
const vscode = require('vscode');
const lsp = require('vscode-languageclient/node');

let client;

function activate(context) {
    const jar = vscode.workspace.getConfiguration('st2c').get('jarPath')
        || context.asAbsolutePath('../../java/target/st2c-jar-with-dependencies.jar');

    const serverOptions = {
        run: { command: 'java', args: ['-cp', jar, 'com.st2c.lsp.ST2CLanguageServer'] },
        debug: { command: 'java', args: ['-cp', jar, 'com.st2c.lsp.ST2CLanguageServer'] },
    };

    const clientOptions = {
        documentSelector: [{ language: 'st' }],
        synchronize: { configurationSection: 'st2c' },
    };

    client = new lsp.LanguageClient('st2c', 'ST2C', serverOptions, clientOptions);
    client.start();
}

function deactivate() {
    if (client) return client.stop();
}

module.exports = { activate, deactivate };
