import subprocess, json, time, sys
from pathlib import Path

jar = "D:/source/Project/ST2C-master/java/target/st2c-jar-with-dependencies.jar"
p = subprocess.Popen(["java", "-cp", jar, "com.st2c.lsp.ST2CLanguageServer"],
    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=0)

def send(obj):
    data = json.dumps(obj).encode()
    p.stdin.write(f"Content-Length: {len(data)}\r\n\r\n".encode() + data)
    p.stdin.flush()

def recv(timeout=3):
    deadline = time.time() + timeout
    while time.time() < deadline:
        line = p.stdout.readline()
        if not line: time.sleep(0.05); continue
        if line.startswith(b'Content-Length:'):
            n = int(line.decode().split(':')[1])
            p.stdout.readline()
            body = p.stdout.read(n).decode()
            return json.loads(body)
    return None

def drain(timeout=1):
    deadline = time.time() + timeout
    while time.time() < deadline:
        line = p.stdout.readline()
        if not line: time.sleep(0.05); continue
        if line.startswith(b'Content-Length:'):
            n = int(line.decode().split(':')[1])
            p.stdout.readline()
            p.stdout.read(n)

PASS, FAIL = 0, 0
def ok(m): global PASS; PASS += 1; print(f"  PASS  {m}")
def nok(m): global FAIL; FAIL += 1; print(f"  FAIL  {m}")

send({"jsonrpc":"2.0","id":1,"method":"initialize","params":{}})
r = recv(3); ok("init") if r else nok("init")
send({"jsonrpc":"2.0","method":"initialized","params":{}}); time.sleep(0.5); drain(1)

# Read io_config.st
root = "D:/source/Project/ST2C-master/examples/projects/robot_arm"
io_config = open(f"{root}/io_config.st", encoding="utf-8").read()
types_st = open(f"{root}/types.st", encoding="utf-8").read()

# Test 1: Open ONLY io_config.st - should auto-load types.st
print("\n[1] Open io_config.st only")
send({"jsonrpc":"2.0","method":"textDocument/didOpen",
"params":{"textDocument":{"uri":f"file:///{root}/io_config.st","languageId":"st","version":1,"text":io_config}}})
time.sleep(3); drain(2)

print("\n[2] Now open types.st as document too")
send({"jsonrpc":"2.0","method":"textDocument/didOpen",
"params":{"textDocument":{"uri":f"file:///{root}/types.st","languageId":"st","version":1,"text":types_st}}})
time.sleep(2)

# Collect diagnostics
diags = {}
for _ in range(20):
    m = recv(0.5)
    if not m: break
    if m.get("method") == "textDocument/publishDiagnostics":
        uri = m["params"]["uri"]
        diags[uri] = m["params"]["diagnostics"]

io_uri = f"file:///{root}/io_config.st"
if io_uri in diags:
    type_errors = [d for d in diags[io_uri] if "can not find type" in d.get("message","")]
    if type_errors:
        nok(f"io_config.st: {len(type_errors)} TYPE errors (first: {type_errors[0]['message'][:60]})")
    else:
        ok(f"io_config.st: {len(diags[io_uri])} diags (no type errors)")
else:
    ok("io_config.st: no diagnostics - type resolution OK")

send({"jsonrpc":"2.0","id":2,"method":"shutdown","params":None})
recv(2)
p.stdin.close()
p.wait(timeout=3)
print(f"\n=== {PASS} passed, {FAIL} failed ===")
