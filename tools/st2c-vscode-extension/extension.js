const path = require('path');
const vscode = require('vscode');
const lsp = require('vscode-languageclient/node');
const { spawn, execSync } = require('child_process');
const fs = require('fs');

let client;
let outputChannel;

function resolveJar(context) {
    return vscode.workspace.getConfiguration('st2c').get('jarPath')
        || context.asAbsolutePath('../../java/target/st2c-jar-with-dependencies.jar');
}

function resolveOutputDir() {
    const raw = vscode.workspace.getConfiguration('st2c').get('outputDir');
    if (!raw) return '${workspaceFolder}/output/flat/build';
    return raw.replace(/\$\{workspaceFolder\}/g, vscode.workspace.rootPath || '');
}

function resolveMakeTarget() {
    return vscode.workspace.getConfiguration('st2c').get('makeTarget') || 'desktop';
}

function resolveMakePath() {
    return vscode.workspace.getConfiguration('st2c').get('makePath') || 'mingw32-make';
}

function resolveGdbPath() {
    return vscode.workspace.getConfiguration('st2c').get('gdbPath') || 'gdb.exe';
}

function getWorkspaceRoot() {
    return vscode.workspace.rootPath || process.cwd();
}

function log(msg) {
    if (!outputChannel) return;
    outputChannel.appendLine(msg);
}

function runJavaJar(jar, args, cwd) {
    return new Promise((resolve, reject) => {
        const child = spawn('java', ['-jar', jar, ...args], { cwd, shell: true });
        let stdout = '', stderr = '';
        child.stdout.on('data', d => { stdout += d.toString(); log(d.toString().trimEnd()); });
        child.stderr.on('data', d => { stderr += d.toString(); log(d.toString().trimEnd()); });
        child.on('close', code => {
            if (code === 0) resolve(stdout);
            else reject(new Error(`java exited with code ${code}\n${stderr}`));
        });
        child.on('error', reject);
    });
}

function runMake(args, cwd) {
    const make = resolveMakePath();
    return new Promise((resolve, reject) => {
        const child = spawn(make, args, { cwd, shell: true });
        let stdout = '', stderr = '';
        child.stdout.on('data', d => { stdout += d.toString(); log(d.toString().trimEnd()); });
        child.stderr.on('data', d => { stderr += d.toString(); log(d.toString().trimEnd()); });
        child.on('close', code => {
            if (code === 0) resolve(stdout);
            else reject(new Error(`make exited with code ${code}`));
        });
        child.on('error', reject);
    });
}

async function ensureJar(jar) {
    if (!fs.existsSync(jar)) {
        const build = await vscode.window.showWarningMessage(
            `st2c JAR not found at ${jar}. Build it now?`,
            'Build JAR', 'Cancel'
        );
        if (build !== 'Build JAR') return false;
        log('Building JAR via Maven...');
        const javaDir = path.resolve(path.dirname(jar), '../..');
        await runJavaJar(
            path.join(path.dirname(jar), '../../../lib/maven/maven-embedder.jar'),
            [],
            javaDir
        );
        try {
            execSync('mvn package -DskipTests', { cwd: javaDir, stdio: 'pipe' });
        } catch (e) {
            throw new Error(`Maven build failed: ${e.message}`);
        }
        if (!fs.existsSync(jar)) {
            vscode.window.showErrorMessage('JAR build failed: output not found');
            return false;
        }
    }
    return true;
}

async function doCompile(jar, flags) {
    const editor = vscode.window.activeTextEditor;
    if (!editor) { vscode.window.showWarningMessage('No active editor'); return; }

    const input = editor.document.fileName;
    if (!input.endsWith('.st')) { vscode.window.showWarningMessage('Not an .st file'); return; }

    const outputDir = resolveOutputDir();
    fs.mkdirSync(outputDir, { recursive: true });

    // Clean stale .cpp/.h files from previous runs to prevent Makefile
    // $(wildcard *.cpp) from pulling in unrelated test snapshots.
    const oldFiles = fs.readdirSync(outputDir)
        .filter(f => f.endsWith('.cpp') || f.endsWith('.h'));
    for (const f of oldFiles) {
        fs.unlinkSync(path.join(outputDir, f));
    }

    const args = [
        '--input', input,
        '--output-dir', outputDir,
        '--verbose',
        ...flags,
    ];

    log(`[ST2C] Compiling: ${input}`);
    log(`[ST2C] Output:   ${outputDir}`);
    await runJavaJar(jar, args, getWorkspaceRoot());
    log('[ST2C] Compilation OK');
    vscode.window.showInformationMessage('ST2C: Compilation successful');
}

function activate(context) {
    outputChannel = vscode.window.createOutputChannel('ST2C');

    // ─── LSP client ───
    const jar = resolveJar(context);
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

    // ─── Command: compile ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.compileFile', async () => {
        try {
            outputChannel.show();
            const j = resolveJar(context);
            if (!(await ensureJar(j))) return;
            await doCompile(j, []);
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Compile failed — ${e.message}`);
        }
    }));

    // ─── Command: compile for debug (with #line directives) ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.compileDebug', async () => {
        try {
            outputChannel.show();
            const j = resolveJar(context);
            if (!(await ensureJar(j))) return;
            await doCompile(j, ['--emit-line-directives', '--generate-debug']);
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Debug compile failed — ${e.message}`);
        }
    }));

    // ─── Command: build runtime (make debug-server) ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.buildRuntime', async () => {
        try {
            outputChannel.show();
            const target = resolveMakeTarget();
            const targetDir = context.asAbsolutePath(`../../target/${target}`);
            if (!fs.existsSync(targetDir)) {
                vscode.window.showErrorMessage(`Target directory not found: ${targetDir}`);
                return;
            }
            log(`[ST2C] Building runtime (${target}/debug-server)...`);
            await runMake(['debug-server'], targetDir);
            log('[ST2C] Runtime build OK');
            vscode.window.showInformationMessage('ST2C: Runtime build successful');
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Runtime build failed — ${e.message}`);
        }
    }));

    // ─── Command: compile + build + debug (one-click) ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.debugRuntime', async () => {
        try {
            outputChannel.show();
            const j = resolveJar(context);
            if (!(await ensureJar(j))) return;

            // Step 1: compile with debug flags
            log('=== Step 1: Compile with #line directives ===');
            await doCompile(j, ['--emit-line-directives', '--generate-debug']);

            // Step 2: build runtime
            log('=== Step 2: Build runtime debug-server ===');
            const target = resolveMakeTarget();
            const targetDir = context.asAbsolutePath(`../../target/${target}`);
            await runMake(['debug-server'], targetDir);

            // Step 3: launch debug
            log('=== Step 3: Launch GDB ===');
            const exeName = target === 'desktop'
                ? 'plc_runtime_desktop_dbg.exe'
                : 'plc_runtime_windows_dbg.exe';
            const exePath = path.join(targetDir, 'build', exeName);
            if (!fs.existsSync(exePath)) {
                throw new Error(`Executable not found: ${exePath}`);
            }

            const wsFolder = getWorkspaceRoot();
            await vscode.debug.startDebugging(undefined, {
                type: 'cppdbg',
                name: 'ST2C Debug',
                request: 'launch',
                program: exePath,
                args: ['--cycle-us', '1000'],
                cwd: wsFolder,
                MIMode: 'gdb',
                miDebuggerPath: resolveGdbPath(),
                setupCommands: [
                    {
                        description: 'Enable pretty-printing',
                        text: '-enable-pretty-printing',
                        ignoreFailures: true,
                    },
                ],
            });
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Debug launch failed — ${e.message}`);
        }
    }));

    // ─── Debug configuration provider ───
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider('st2c', {
        provideDebugConfigurations(folder) {
            return [
                {
                    type: 'cppdbg',
                    name: 'ST2C: Debug PLC Runtime (GDB)',
                    request: 'launch',
                    program: '${workspaceFolder}/target/desktop/build/plc_runtime_desktop_dbg.exe',
                    args: ['--cycle-us', '1000'],
                    cwd: '${workspaceFolder}',
                    MIMode: 'gdb',
                    miDebuggerPath: resolveGdbPath(),
                    setupCommands: [
                        {
                            description: 'Enable pretty-printing',
                            text: '-enable-pretty-printing',
                            ignoreFailures: true,
                        },
                    ],
                },
            ];
        },
        resolveDebugConfiguration(folder, config) {
            if (!config.type && !config.request && !config.name) {
                const editor = vscode.window.activeTextEditor;
                if (editor && editor.document.languageId === 'st') {
                    return {
                        type: 'cppdbg',
                        name: 'ST2C: Debug PLC Runtime (GDB)',
                        request: 'launch',
                        program: '${workspaceFolder}/target/desktop/build/plc_runtime_desktop_dbg.exe',
                        args: ['--cycle-us', '1000'],
                        cwd: '${workspaceFolder}',
                        MIMode: 'gdb',
                        miDebuggerPath: resolveGdbPath(),
                        setupCommands: [
                            {
                                description: 'Enable pretty-printing',
                                text: '-enable-pretty-printing',
                                ignoreFailures: true,
                            },
                        ],
                    };
                }
            }
            return config;
        },
    }));
}

function deactivate() {
    if (client) return client.stop();
}

module.exports = { activate, deactivate };
