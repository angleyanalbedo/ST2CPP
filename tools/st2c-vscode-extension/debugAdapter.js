#!/usr/bin/env node
// ST2C Debug Adapter — GDB/MI backend for debugging ST source files
//
// Communicates with VS Code via Debug Adapter Protocol (stdin/stdout JSON-RPC)
// Communicates with GDB via GDB/MI interpreter (child process)

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const logPath = path.join(__dirname, '..', 'debug_adapter.log');
const logFd = fs.openSync(logPath, 'a');
function log(msg) {
    fs.writeSync(logFd, `[${new Date().toISOString()}] ${msg}\n`);
}

log("--- Debug Adapter Started ---");

// ─── MI2 value parser ──────────────────────────────────────────────────────

function parseMIValue(v) {
    v = (v || '').trim();
    if (v.startsWith('"')) {
        let s = '';
        for (let i = 1; i < v.length - 1; i++) {
            if (v[i] === '\\') { i++; const c = v[i] || ''; s += c === 'n' ? '\n' : c; }
            else s += v[i];
        }
        return s;
    }
    if (v.startsWith('{')) {
        const obj = {};
        const inner = v.slice(1, -1).trim();
        if (inner) {
            let depth = 0, start = 0, inKey = true, key = '';
            for (let i = 0; i < inner.length; i++) {
                const ch = inner[i];
                if (ch === '{' || ch === '[' || ch === '"') {
                    depth++;
                    while (i < inner.length) { if (inner[i] === '"' && inner[i - 1] !== '\\') depth--; if (depth === 0) break; i++; }
                }
                if (ch === '=' && inKey && depth === 0) {
                    key = inner.slice(start, i).trim();
                    start = i + 1;
                    inKey = false;
                } else if (ch === ',' && depth === 0) {
                    obj[key] = parseMIValue(inner.slice(start, i));
                    key = '';
                    start = i + 1;
                    inKey = true;
                }
            }
            if (key) obj[key] = parseMIValue(inner.slice(start));
        }
        return obj;
    }
    if (v.startsWith('[')) {
        const arr = [];
        const inner = v.slice(1, -1).trim();
        if (inner) {
            let depth = 0, start = 0;
            for (let i = 0; i < inner.length; i++) {
                const ch = inner[i];
                if (ch === '{' || ch === '[' || ch === '"') {
                    depth++;
                    while (i < inner.length) { if (inner[i] === '"' && inner[i - 1] !== '\\') depth--; if (depth === 0) break; i++; }
                } else if (ch === ',' && depth === 0) {
                    let item = inner.slice(start, i).trim();
                    let eqIdx = item.indexOf('=');
                    if (eqIdx > 0 && item[0] !== '{' && item[0] !== '"') {
                        let k = item.slice(0, eqIdx).trim();
                        let v = parseMIValue(item.slice(eqIdx + 1));
                        let obj = {}; obj[k] = v;
                        arr.push(obj);
                    } else {
                        arr.push(parseMIValue(item));
                    }
                    start = i + 1;
                }
            }
            if (start < inner.length) {
                let item = inner.slice(start).trim();
                let eqIdx = item.indexOf('=');
                if (eqIdx > 0 && item[0] !== '{' && item[0] !== '"') {
                    let k = item.slice(0, eqIdx).trim();
                    let v = parseMIValue(item.slice(eqIdx + 1));
                    let obj = {}; obj[k] = v;
                    arr.push(obj);
                } else {
                    arr.push(parseMIValue(item));
                }
            }
        }
        return arr;
    }
    return v;
}

function parseMIResults(line) {
    const r = {};
    const eqIdx = line.indexOf(',');
    if (eqIdx < 0) return r;
    let rest = line.slice(eqIdx + 1);
    let depth = 0, start = 0, inKey = true, key = '';
    for (let i = 0; i < rest.length; i++) {
        const ch = rest[i];
        if (ch === '{' || ch === '[' || ch === '"') {
            depth++;
            while (i < rest.length) { const c2 = rest[++i]; if (c2 === '"' && rest[i - 1] !== '\\') depth--; if (depth === 0) break; }
        } else if (ch === '=' && inKey && depth === 0) {
            key = rest.slice(start, i).trim();
            start = i + 1;
            inKey = false;
        } else if (ch === ',' && depth === 0) {
            r[key] = parseMIValue(rest.slice(start, i));
            key = '';
            start = i + 1;
            inKey = true;
        }
    }
    if (key) r[key] = parseMIValue(rest.slice(start));
    return r;
}

function classifyMILine(line) {
    if (!line || line.trim() === '(gdb)') return 'prompt';
    if (line.startsWith('^')) return 'result';
    if (line.startsWith('*')) return 'exec';
    if (line.startsWith('=')) return 'notify';
    if (line.startsWith('~') || line.startsWith('&') || line.startsWith('@')) return 'stream';
    return 'other';
}

// ─── GDB Client ────────────────────────────────────────────────────────────

class GDBClient {
    constructor(gdbPath, onEvent) {
        this.gdbPath = gdbPath || 'gdb.exe';
        this.onEvent = onEvent;
        this.buffer = '';
        this.pendingResolve = null;
        this.seq = 0;
        this.cmdQueue = [];
        this.cmdActive = false;
    }

    async start(executable, args) {
        log(`GDB start: executable=${executable}, args=${JSON.stringify(args)}`);
        this.proc = spawn(this.gdbPath, ['--interpreter=mi2'], {
            stdio: ['pipe', 'pipe', 'pipe'],
            shell: true,
        });
        this.proc.on('exit', (code) => {
            log(`GDB exited with code ${code}`);
            if (this.pendingResolve) { this.pendingResolve({ _class: 'error' }); this.pendingResolve = null; }
            while(this.cmdQueue.length > 0) {
                const item = this.cmdQueue.shift();
                item.reject(new Error('GDB exited'));
            }
            if (this.onEvent) this.onEvent({ _class: 'exit', code });
        });
        this.proc.stdout.on('data', data => {
            const str = data.toString();
            log(`GDB OUT: ${str.trim()}`);
            this._onData(str);
        });
        this.proc.stderr.on('data', data => {
            log(`GDB ERR: ${data.toString().trim()}`);
        });

        // Discard initial version output
        await this._waitForPrompt(5000);

        // Load executable
        await this.sendCommand(`-file-exec-and-symbols "${executable}"`);
        if (args) await this.sendCommand(`-exec-arguments ${args.map(a => `"${a}"`).join(' ')}`);
    }

    _onData(chunk) {
        this.buffer += chunk;
        // Process up to each (gdb) prompt
        let idx;
        while ((idx = this.buffer.indexOf('\n(gdb) \n')) >= 0 || (idx = this.buffer.indexOf('(gdb) \n')) >= 0) {
            const block = this.buffer.slice(0, idx);
            this.buffer = this.buffer.slice(idx + (block.endsWith('\n') ? 7 : 6));
            this._processBlock(block);
        }
    }

    _processBlock(block) {
        const lines = block.split('\n').filter(l => l.trim());
        for (const line of lines) {
            const cat = classifyMILine(line);
            if (cat === 'result') {
                const r = { _class: line[0] === '^' ? line.slice(1, line.indexOf(',') >= 0 ? line.indexOf(',') : undefined) : 'done', ...parseMIResults(line) };
                if (this.pendingResolve) { 
                    const resolve = this.pendingResolve;
                    this.pendingResolve = null; 
                    this.cmdActive = false;
                    resolve(r); 
                    setImmediate(() => this._pump());
                }
                else if (this.onEvent) this.onEvent(r);
            } else if (cat === 'exec' || cat === 'notify') {
                const ev = { _class: line[0], _reason: line.indexOf(',') >= 0 ? line.slice(1, line.indexOf(',')) : line.slice(1), ...parseMIResults(line) };
                if (this.onEvent) this.onEvent(ev);
            }
        }
    }

    _waitForPrompt(timeout) {
        return new Promise((resolve) => {
            const timer = setTimeout(() => resolve(), timeout);
            const check = () => {
                const idx = this.buffer.indexOf('(gdb)');
                if (idx >= 0) { this.buffer = this.buffer.slice(idx + 6); clearTimeout(timer); resolve(); }
                else setImmediate(check);
            };
            check();
        });
    }

    async sendCommand(cmd) {
        return new Promise((resolve, reject) => {
            this.cmdQueue.push({ cmd, resolve, reject });
            this._pump();
        });
    }

    _pump() {
        if (this.cmdActive || this.cmdQueue.length === 0) return;
        this.cmdActive = true;
        const { cmd, resolve, reject } = this.cmdQueue.shift();
        this.pendingResolve = resolve;
        log(`GDB CMD: ${cmd}`);
        this.proc.stdin.write(cmd + '\n');
        setTimeout(() => {
            if (this.pendingResolve === resolve) { 
                log(`GDB CMD Timeout: ${cmd}`);
                this.pendingResolve({ _class: 'error', msg: 'timeout' }); 
                this.pendingResolve = null; 
                this.cmdActive = false;
                this._pump();
            }
        }, 10000);
    }

    async sendCLI(cmd) {
        return this.sendCommand(`-interpreter-exec console "${cmd.replace(/"/g, '\\"')}"`);
    }

    quit() {
        if (this.proc) { this.proc.kill(); this.proc = null; }
    }
}

// ─── Debug Adapter ─────────────────────────────────────────────────────────

const WARN = process.env.VSCODE_DEBUG ? console.warn : () => {};

class ST2CDebugAdapter {
    constructor() {
        this._buf = '';
        this._pending = new Map();
        this._seq = 0;
        this._gdb = null;
        this._started = false;
        this._breakpoints = new Map();
    }

    // ── DAP request dispatch ──

    async handleRequest(req) {
        log(`DAP REQ: ${JSON.stringify(req)}`);
        const { command, arguments: args, seq } = req;
        try {
            if (typeof this[`_${command}`] !== 'function') {
                log(`DAP REQ unhandled: ${command}`);
                this._send({ type: 'response', request_seq: seq, success: false, command, message: `unsupported command: ${command}` });
                return;
            }
            const body = await this[`_${command}`](args);
            this._send({ type: 'response', request_seq: seq, success: true, command, body });
        } catch (e) {
            log(`DAP REQ ERROR: ${e.stack}`);
            this._send({ type: 'response', request_seq: seq, success: false, command, message: e.message });
        }
    }

    // ── DAP handlers ──

    async _initialize(args) {
        return {
            supportsConfigurationDoneRequest: true,
            supportsSetVariable: false,
            supportsConditionalBreakpoints: false,
            supportsFunctionBreakpoints: false,
            supportsEvaluateForHovers: false,
            supportTerminateDebuggee: true,
        };
    }

    async _launch(args) {
        const program = args.program;
        const programArgs = args.args || [];
        const miDebuggerPath = args.miDebuggerPath || 'gdb.exe';

        this._gdb = new GDBClient(miDebuggerPath, ev => this._onGDBevent(ev));
        await this._gdb.start(program, programArgs);

        // Set breakpoint pending by default
        await this._gdb.sendCLI('set breakpoint pending on');

        // Removed SIGTRAP ignore rule because it breaks Windows breakpoints

        this._send({ type: 'event', event: 'initialized' });
    }

    async _threads(args) {
        return {
            threads: [
                { id: 1, name: "Main Thread" }
            ]
        };
    }

    async _setExceptionBreakpoints(args) {
        return {};
    }

    async _setBreakpoints(args) {
        const { source: { path: srcPath }, breakpoints: bps } = args;
        const normalized = srcPath.replace(/\\/g, '/').replace(/^[A-Z]:/, m => m.toLowerCase());
        const result = [];

        // Remove old breakpoints for this file
        const oldBps = this._breakpoints.get(normalized) || [];
        for (const old of oldBps) {
            await this._gdb.sendCommand(`-break-delete ${old.id}`).catch(() => {});
        }

        // Set new breakpoints
        const newBps = [];
        if (bps) {
            for (const bp of bps) {
                const line = bp.line;
                try {
                    const resp = await this._gdb.sendCommand(`-break-insert -f --source "${normalized}" --line ${line}`);
                    const bkpt = resp.bkpt || {};
                    newBps.push({
                        id: parseInt(bkpt.number || 0),
                        line: bp.line,
                        verified: true,
                        source: { path: srcPath },
                    });
                } catch (e) {
                    newBps.push({ id: 0, line, verified: false, message: e.message });
                }
            }
        }
        this._breakpoints.set(normalized, newBps);
        return { breakpoints: newBps };
    }

    async _configurationDone(args) {
        this._started = true;
        const resp = await this._gdb.sendCommand('-exec-run');
        if (resp._class === 'error') {
            // Try start if run fails (some systems need stop at main)
            await this._gdb.sendCommand('-exec-start');
        }
    }

    async _continue(args) {
        await this._gdb.sendCommand('-exec-continue');
        return { allThreadsContinued: true };
    }

    async _next(args) {
        await this._gdb.sendCommand('-exec-next');
        return {};
    }

    async _stepIn(args) {
        await this._gdb.sendCommand('-exec-step');
        return {};
    }

    async _stepOut(args) {
        await this._gdb.sendCommand('-exec-finish');
        return {};
    }

    async _pause(args) {
        await this._gdb.sendCommand('-exec-interrupt');
        return {};
    }

    async _stackTrace(args) {
        const resp = await this._gdb.sendCommand('-stack-list-frames');
        const stackList = Array.isArray(resp.stack) ? resp.stack : [(resp.stack || {})];
        const stack = stackList.map(s => s.frame || s).filter(f => Object.keys(f).length > 0);
        const frames = stack.map((f, i) => ({
            id: i,
            name: f.func || '??',
            source: f.file && f.file !== '??' ? {
                path: f.file || f.fullname || '',
                name: path.basename(f.file || f.fullname || ''),
                sourceReference: 0,
            } : undefined,
            line: parseInt(f.line) || 0,
            column: 1,
        }));
        return { stackFrames: frames, totalFrames: frames.length };
    }

    async _scopes(args) {
        return {
            scopes: [
                { name: 'Locals', variablesReference: 1, expensive: false },
            ],
        };
    }

    async _variables(args) {
        if (args.variablesReference === 1) {
            const resp = await this._gdb.sendCommand('-stack-list-variables --all-values');
            const vars = (resp.variables || []).map(v => ({
                name: v.name || v.var || '',
                value: v.value || '??',
                variablesReference: 0,
            }));
            return { variables: vars };
        }
        return { variables: [] };
    }

    async _evaluate(args) {
        const resp = await this._gdb.sendCommand(`-data-evaluate-expression "${args.expression.replace(/"/g, '\\"')}"`);
        return { result: resp.value || '??', variablesReference: 0 };
    }

    async _disconnect(args) {
        if (this._gdb) {
            await this._gdb.sendCLI('quit').catch(() => {});
            this._gdb.quit();
            this._gdb = null;
        }
        return {};
    }

    // ── GDB async events ──

    _onGDBevent(ev) {
        if (ev._class === '*') {
            const reason = ev._reason || '';
            if (reason === 'stopped') {
                const frame = ev.frame || {};
                this._send({
                    type: 'event',
                    event: 'stopped',
                    body: {
                        reason: 'breakpoint',
                        threadId: 1,
                        allThreadsStopped: true,
                        description: ev.reason || '',
                        text: ev.reason || '',
                        hitBreakpointIds: ev.bkptno ? [parseInt(ev.bkptno)] : [],
                        source: frame.file ? {
                            path: frame.file || frame.fullname || '',
                            name: path.basename(frame.file || frame.fullname || ''),
                        } : undefined,
                        line: parseInt(frame.line) || 0,
                        column: 1,
                    },
                });
            }
        } else if (ev._class === '=' && ev._reason === 'breakpoint-created') {
            // GDB resolved a pending breakpoint
        } else if (ev._class === 'exit') {
            this._send({ type: 'event', event: 'terminated' });
            this._send({ type: 'event', event: 'exited', body: { exitCode: ev.code || 0 } });
        }
    }

    // ── Message I/O ──

    _send(msg) {
        msg.type = msg.type || 'event';
        if (msg.type === 'response') msg.seq = ++this._seq;
        const json = JSON.stringify(msg);
        log(`DAP OUT: ${json}`);
        const header = `Content-Length: ${Buffer.byteLength(json, 'utf-8')}\r\n\r\n`;
        process.stdout.write(header + json);
    }

    feedChunk(chunk) {
        this._buf += chunk;
        while (true) {
            const match = this._buf.match(/Content-Length:\s*(\d+)\r?\n\r?\n/);
            if (!match) return;
            const len = parseInt(match[1]);
            const start = match.index + match[0].length;
            if (this._buf.length < start + len) return;
            const body = this._buf.slice(start, start + len);
            this._buf = this._buf.slice(start + len);
            try {
                const msg = JSON.parse(body);
                if (msg.type === 'request') this.handleRequest(msg);
            } catch (e) {
                WARN('Failed to parse DAP message:', e.message);
            }
        }
    }
}

// ─── Main ───────────────────────────────────────────────────────────────────

const adapter = new ST2CDebugAdapter();
process.stdin.on('data', chunk => {
    log(`STDIN chunk: ${chunk.length} bytes`);
    adapter.feedChunk(chunk.toString());
});
process.stdin.on('end', () => {
    log('STDIN ended, exiting');
    process.exit(0);
});
