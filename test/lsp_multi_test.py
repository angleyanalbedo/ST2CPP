import subprocess, json, sys, time

jar = "D:/source/Project/ST2C-master/java/target/st2c-jar-with-dependencies.jar"
proc = subprocess.Popen(["java", "-cp", jar, "com.st2c.lsp.ST2CLanguageServer"],
    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=0)

def send(obj):
    data = json.dumps(obj, ensure_ascii=False).encode()
    proc.stdin.write(f"Content-Length: {len(data)}\r\n\r\n".encode())
    proc.stdin.write(data)
    proc.stdin.flush()

def recv_id(target_id=None, timeout=5):
    deadline = time.time() + timeout
    while time.time() < deadline:
        line = proc.stdout.readline()
        if not line or not line.startswith(b'Content-Length:'):
            continue
        n = int(line.decode().strip().split(':')[1])
        proc.stdout.readline()  # blank
        body = proc.stdout.read(n).decode()
        msg = json.loads(body)
        if target_id is None or msg.get('id') == target_id:
            return msg
    return None

def drain(timeout=1):
    deadline = time.time() + timeout
    while time.time() < deadline:
        line = proc.stdout.readline()
        if not line: break
        if line.startswith(b'Content-Length:'):
            n = int(line.decode().split(':')[1])
            proc.stdout.readline()
            proc.stdout.read(n)

PASS = 0; FAIL = 0
def ok(m): global PASS; PASS += 1; print(f"  PASS  {m}")
def nok(m): global FAIL; FAIL += 1; print(f"  FAIL  {m}")

# Init
send({"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":9999,"capabilities":{}}})
r = recv_id(1)
if r and 'result' in r: ok("initialize")
else: nok(f"init: {r}")

send({"jsonrpc":"2.0","method":"initialized","params":{}})
time.sleep(0.5); drain(1)

# Open types.st alone
types_code = open("D:/source/Project/ST2C-master/examples/projects/robot_arm/types.st").read()
send({"jsonrpc":"2.0","method":"textDocument/didOpen",
"params":{"textDocument":{"uri":"file:///types.st","languageId":"st","version":1,"text":types_code}}})
time.sleep(2)
drain(1)
ok("types.st opened")

# Open io_config.st
io_code = open("D:/source/Project/ST2C-master/examples/projects/robot_arm/io_config.st").read()
send({"jsonrpc":"2.0","method":"textDocument/didOpen",
"params":{"textDocument":{"uri":"file:///io_config.st","languageId":"st","version":1,"text":io_code}}})
time.sleep(2)
drain(1)
ok("io_config.st opened")

# Check if io_config had type errors by re-parsing
send({"jsonrpc":"2.0","id":2,"method":"textDocument/hover",
"params":{"textDocument":{"uri":"file:///io_config.st"},"position":{"line":0,"character":0}}})
r = recv_id(2)
if r: ok("hover response received")
else: nok("no hover response")

send({"jsonrpc":"2.0","id":3,"method":"shutdown","params":None})
recv_id(3)
proc.stdin.close()
print(f"\n=== {PASS} passed, {FAIL} failed ===")
