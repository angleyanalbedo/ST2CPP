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

function resolvePythonPath() {
    return vscode.workspace.getConfiguration('st2c').get('pythonPath') || 'python';
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

function runPython(args, cwd) {
    const python = resolvePythonPath();
    return new Promise((resolve, reject) => {
        const child = spawn(python, args, { cwd, shell: true });
        let stdout = '', stderr = '';
        child.stdout.on('data', d => { stdout += d.toString(); log(d.toString().trimEnd()); });
        child.stderr.on('data', d => { stderr += d.toString(); log(d.toString().trimEnd()); });
        child.on('close', code => {
            if (code === 0) resolve(stdout);
            else reject(new Error(`python exited with code ${code}\n${stderr}`));
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

async function doCompile(jar, inputFiles, flags) {
    if (!inputFiles || inputFiles.length === 0) {
        vscode.window.showWarningMessage('No input files');
        return;
    }

    const outputDir = resolveOutputDir();

    // Wipe entire output directory to prevent stale .cpp/.h from polluting
    // Makefile's $(wildcard *.cpp) in desktop/target.
    if (fs.existsSync(outputDir)) {
        fs.rmSync(outputDir, { recursive: true, force: true });
    }
    fs.mkdirSync(outputDir, { recursive: true });

    const args = [
        ...inputFiles.flatMap(f => ['--input', f]),
        '--output-dir', outputDir,
        '--verbose',
        ...flags,
    ];

    log(`[ST2C] Compiling: ${inputFiles.length} file(s)`);
    for (const f of inputFiles) log(`  input: ${f}`);
    log(`[ST2C] Output:   ${outputDir}`);
    await runJavaJar(jar, args, getWorkspaceRoot());
    log('[ST2C] Compilation OK');
}

async function buildRuntime(context) {
    const target = resolveMakeTarget();
    const targetDir = context.asAbsolutePath(`../../target/${target}`);
    if (!fs.existsSync(targetDir)) {
        throw new Error(`Target directory not found: ${targetDir}`);
    }

    // Step: run gen_config.py
    log('[ST2C] Generating runtime config...');
    const scriptDir = context.asAbsolutePath('../../target/scripts');
    await runPython([
        path.join(scriptDir, 'gen_config.py'),
        '--target', target,
    ], scriptDir);

    // Step: make debug-server
    log('[ST2C] Building runtime (debug-server)...');
    await runMake(['debug-server'], targetDir);
    log('[ST2C] Runtime build OK');
}

function activate(context) {
    outputChannel = vscode.window.createOutputChannel('ST2C');

    // Enable breakpoints in .st files for cppdbg debugging
    vscode.workspace.getConfiguration('debug')
        .update('allowBreakpointsEverywhere', true, vscode.ConfigurationTarget.Workspace);

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

    // ─── Command: compile current .st → build runtime ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.compileCurrent', async () => {
        try {
            outputChannel.show();
            const editor = vscode.window.activeTextEditor;
            if (!editor) { vscode.window.showWarningMessage('No active editor'); return; }
            const input = editor.document.fileName;
            if (!input.endsWith('.st')) { vscode.window.showWarningMessage('Not an .st file'); return; }

            const j = resolveJar(context);
            if (!(await ensureJar(j))) return;

            log('=== Step 1: Compile ST → C++ ===');
            await doCompile(j, [input], ['--emit-line-directives', '--generate-debug']);

            log('=== Step 2: Generate runtime config + Build runtime ===');
            await buildRuntime(context);

            vscode.window.showInformationMessage('ST2C: Compile Current + Build successful');
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Compile Current failed — ${e.message}`);
        }
    }));

    // ─── Command: compile all project .st → build runtime ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.compileProject', async () => {
        try {
            outputChannel.show();
            const wsRoot = getWorkspaceRoot();
            const files = await vscode.workspace.findFiles(
                new vscode.RelativePattern(wsRoot, '**/*.st'),
                '**/node_modules/**'
            );
            if (files.length === 0) {
                vscode.window.showWarningMessage('No .st files found in workspace');
                return;
            }

            const j = resolveJar(context);
            if (!(await ensureJar(j))) return;

            log('=== Step 1: Compile all ST files → C++ ===');
            await doCompile(j, files.map(f => f.fsPath), ['--emit-line-directives', '--generate-debug']);

            log('=== Step 2: Generate runtime config + Build runtime ===');
            await buildRuntime(context);

            vscode.window.showInformationMessage('ST2C: Compile Project + Build successful');
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Compile Project failed — ${e.message}`);
        }
    }));

    // ─── Command: compile current .st → build runtime → launch GDB ───
    context.subscriptions.push(vscode.commands.registerCommand('st2c.debugRuntime', async () => {
        try {
            outputChannel.show();
            const editor = vscode.window.activeTextEditor;
            if (!editor) { vscode.window.showWarningMessage('No active editor'); return; }
            const input = editor.document.fileName;
            if (!input.endsWith('.st')) { vscode.window.showWarningMessage('Not an .st file'); return; }

            const j = resolveJar(context);
            if (!(await ensureJar(j))) return;

            log('=== Step 1: Compile ST → C++ (debug) ===');
            await doCompile(j, [input], ['--emit-line-directives', '--generate-debug']);

            log('=== Step 2: Generate runtime config + Build runtime ===');
            await buildRuntime(context);

            log('=== Step 3: Launch GDB ===');
            const target = resolveMakeTarget();
            const targetDir = context.asAbsolutePath(`../../target/${target}`);
            const exeName = target === 'desktop'
                ? 'plc_runtime_desktop_dbg.exe'
                : 'plc_runtime_windows_dbg.exe';
            const exePath = path.join(targetDir, 'build', exeName);
            if (!fs.existsSync(exePath)) {
                throw new Error(`Executable not found: ${exePath}`);
            }

            await vscode.debug.startDebugging(undefined, {
                type: 'cppdbg',
                name: 'ST2C Debug',
                request: 'launch',
                program: exePath,
                args: ['--cycle-us', '1000'],
                cwd: getWorkspaceRoot(),
                targetArchitecture: 'x86_64',
                MIMode: 'gdb',
                miDebuggerPath: resolveGdbPath(),
                setupCommands: [
                    {
                        description: 'Enable pretty-printing',
                        text: '-enable-pretty-printing',
                        ignoreFailures: true,
                    },
                    {
                        description: 'Ignore SIGTRAP (debug server thread)',
                        text: 'handle SIGTRAP nostop noprint',
                        ignoreFailures: true,
                    },
                ],
                customLaunchSetupCommands: [
                    {
                        description: 'Enable pending breakpoints',
                        text: 'set breakpoint pending on',
                        ignoreFailures: true,
                    },
                    {
                        description: 'Run (no stop at main)',
                        text: 'run',
                        ignoreFailures: false,
                    },
                ],
            });
        } catch (e) {
            log(`[ERROR] ${e.message}`);
            vscode.window.showErrorMessage(`ST2C: Debug failed — ${e.message}`);
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
                    targetArchitecture: 'x86_64',
                    MIMode: 'gdb',
                    miDebuggerPath: resolveGdbPath(),
                    setupCommands: [
                        { description: 'Enable pretty-printing', text: '-enable-pretty-printing', ignoreFailures: true },
                        { description: 'Ignore SIGTRAP', text: 'handle SIGTRAP nostop noprint', ignoreFailures: true },
                    ],
                    customLaunchSetupCommands: [
                        { description: 'Enable pending breakpoints', text: 'set breakpoint pending on', ignoreFailures: true },
                        { description: 'Run (no stop at main)', text: 'run', ignoreFailures: false },
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
                        targetArchitecture: 'x86_64',
                        MIMode: 'gdb',
                        miDebuggerPath: resolveGdbPath(),
                        setupCommands: [
                            { description: 'Enable pretty-printing', text: '-enable-pretty-printing', ignoreFailures: true },
                            { description: 'Ignore SIGTRAP', text: 'handle SIGTRAP nostop noprint', ignoreFailures: true },
                        ],
                        customLaunchSetupCommands: [
                            { description: 'Enable pending breakpoints', text: 'set breakpoint pending on', ignoreFailures: true },
                            { description: 'Run (no stop at main)', text: 'run', ignoreFailures: false },
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
