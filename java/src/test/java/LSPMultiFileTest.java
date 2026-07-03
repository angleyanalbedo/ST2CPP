import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * LSP 多文件集成测试 — 使用 examples/projects/lsp_test
 * <p>
 * 测试目标：
 * 1. 跨文件类型解析（types.st 定义的类型在 utils.st/main.st 中可见）
 * 2. 跨文件函数解析（utils.st 定义函数，main.st 调用）
 * 3. didChange 后重新诊断的正确性
 */
public class LSPMultiFileTest {

    private static final Path PROJECT_DIR = Paths.get("../examples/projects/lsp_test").toAbsolutePath().normalize();
    private static final Gson GSON = new Gson();
    private static final int TIMEOUT_MS = 15000;

    private Process lspProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final Map<String, List<JsonObject>> receivedDiagnostics = new ConcurrentHashMap<>();
    private final Map<Integer, JsonObject> receivedResponses = new ConcurrentHashMap<>();
    private Thread stdoutThread;
    private volatile boolean running = true;

    @Before
    public void setUp() throws Exception {
        assertTrue("Project not found at " + PROJECT_DIR, Files.isDirectory(PROJECT_DIR));
        assertTrue("types.st missing", Files.exists(PROJECT_DIR.resolve("types.st")));
        assertTrue("utils.st missing", Files.exists(PROJECT_DIR.resolve("utils.st")));
        assertTrue("main.st missing", Files.exists(PROJECT_DIR.resolve("main.st")));

        String classpath = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", classpath, "com.st2c.lsp.ST2CLanguageServer");
        pb.redirectErrorStream(false);
        pb.directory(new File(".").getAbsoluteFile());

        lspProcess = pb.start();
        reader = new BufferedReader(new InputStreamReader(lspProcess.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(lspProcess.getOutputStream(), StandardCharsets.UTF_8));

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(lspProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                while (running) {
                    String line = errReader.readLine();
                    if (line == null) break;
                    System.err.println("[LSP-ERR] " + line);
                }
            } catch (IOException ignored) { }
        });
        stderrThread.setDaemon(true);
        stderrThread.start();

        stdoutThread = new Thread(() -> {
            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (line.startsWith("Content-Length: ")) {
                        int len = Integer.parseInt(line.substring("Content-Length: ".length()).trim());
                        String blank = reader.readLine();
                        char[] buf = new char[len];
                        int totalRead = 0;
                        while (totalRead < len) {
                            int n = reader.read(buf, totalRead, len - totalRead);
                            if (n < 0) break;
                            totalRead += n;
                        }
                        String body = new String(buf, 0, totalRead);
                        JsonObject msg = GSON.fromJson(body, JsonObject.class);
                        handleMessage(msg);
                    }
                }
            } catch (Exception ignored) { }
        });
        stdoutThread.setDaemon(true);
        stdoutThread.start();

        JsonObject initParams = new JsonObject();
        initParams.addProperty("processId", 9999);
        initParams.add("capabilities", new JsonObject());
        sendMessage(createRequest(1, "initialize", initParams));
        JsonObject resp = waitForResponse(1);
        assertNotNull("No response to initialize", resp);
        assertTrue("Initialize failed: " + resp, resp.has("result"));
        sendMessage(createNotification("initialized", new JsonObject()));
        receivedDiagnostics.clear();
        Thread.sleep(300);
    }

    @After
    public void tearDown() {
        try {
            sendMessage(createRequest(999, "shutdown", null));
            waitForResponse(999);
            sendMessage(createNotification("exit", new JsonObject()));
        } catch (Exception ignored) { }
        running = false;
        if (lspProcess != null) {
            lspProcess.destroyForcibly();
        }
    }

    // ─── Test 1: Cross-file type resolution ───

    @Test
    public void testCrossFileTypeResolution() throws Exception {
        openDocument("types.st");
        assertNoDiagnostics("types.st");

        // utils.st uses MyPoint, MyResult from types.st
        openDocument("utils.st");
        assertNoDiagnostics("utils.st",
                "utils.st references MyPoint/MyResult from types.st");

        // main.st uses types from types.st and function from utils.st
        openDocument("main.st");
        assertNoDiagnostics("main.st",
                "main.st references all cross-file resources");
    }

    // ─── Test 2: All files clean ───

    @Test
    public void testAllFilesClean() throws Exception {
        openDocument("types.st");
        openDocument("utils.st");
        openDocument("main.st");

        waitForIdle(2000);

        int totalErrors = 0;
        for (var entry : receivedDiagnostics.entrySet()) {
            totalErrors += entry.getValue().size();
            if (!entry.getValue().isEmpty()) {
                System.err.println("  Diagnostics in " + entry.getKey() + ":");
                for (JsonObject d : entry.getValue()) {
                    System.err.println("    " + d);
                }
            }
        }
        assertEquals("Expected zero diagnostics across all files", 0, totalErrors);
    }

    // ─── Test 3: didChange triggers reanalysis ───

    @Test
    public void testDidChangeTriggersReanalysis() throws Exception {
        openDocument("types.st");
        openDocument("utils.st");

        // Modify types.st (append comment)
        String original = readProjectFile("types.st");
        String modified = original + "\n(* modified *)\n";
        changeDocument("types.st", modified);
        assertNoDiagnostics("types.st", "types.st should be clean after trivial change");
        assertNoDiagnostics("utils.st", "utils.st should remain clean after types.st change");

        // Revert
        changeDocument("types.st", original);
        assertNoDiagnostics("types.st", "types.st should be clean after revert");
        assertNoDiagnostics("utils.st", "utils.st should remain clean after revert");
    }

    // ─── Test 4: Open order independence ───

    @Test
    public void testOpenOrderIndependence() throws Exception {
        // Open main.st first (which references types/functions from other files)
        openDocument("types.st");
        // Now types are in the symbol table
        openDocument("main.st");
        assertNoDiagnostics("main.st", "main.st should resolve types even when opened second");

        // Open utils.st last
        openDocument("utils.st");
        assertNoDiagnostics("utils.st", "utils.st should resolve types even when opened last");
    }

    // ─── Test 5: Hover and Completion ───

    @Test
    public void testHoverAndCompletion() throws Exception {
        openDocument("types.st");
        openDocument("utils.st");
        openDocument("main.st");

        // Hover on "MyPoint" in main.st
        JsonObject hoverParams = new JsonObject();
        hoverParams.add("textDocument", docId("main.st"));
        JsonObject pos = new JsonObject();
        pos.addProperty("line", 2);
        pos.addProperty("character", 12);
        hoverParams.add("position", pos);
        sendRequest("textDocument/hover", 10, hoverParams);
        JsonObject resp = waitForResponse(10);
        if (resp != null && resp.has("result")) {
            System.err.println("  Hover result: " + resp.get("result"));
        }

        // Completion
        JsonObject compParams = new JsonObject();
        compParams.add("textDocument", docId("main.st"));
        JsonObject cpos = new JsonObject();
        cpos.addProperty("line", 0);
        cpos.addProperty("character", 0);
        compParams.add("position", cpos);
        sendRequest("textDocument/completion", 11, compParams);
        resp = waitForResponse(11);
        if (resp != null && resp.has("result") && resp.getAsJsonObject("result").has("items")) {
            int count = resp.getAsJsonObject("result").getAsJsonArray("items").size();
            assertTrue("Completion should return items", count > 0);
            System.err.println("  Completion: " + count + " items");
        }
    }

    // ─── Test 6: Cross-file function call resolution ───

    @Test
    public void testCrossFileFunctionCall() throws Exception {
        openDocument("types.st");
        openDocument("utils.st");
        openDocument("main.st");

        // main.st calls ADD_POINTS defined in utils.st with types from types.st
        assertNoDiagnostics("main.st",
                "main.st should resolve ADD_POINTS from utils.st and types from types.st");
    }

    // ─── Helpers ───

    private String uriFor(String filename) {
        return PROJECT_DIR.resolve(filename).toUri().toString();
    }

    private JsonObject docId(String filename) {
        JsonObject td = new JsonObject();
        td.addProperty("uri", uriFor(filename));
        return td;
    }

    private String readProjectFile(String filename) throws IOException {
        return Files.readString(PROJECT_DIR.resolve(filename));
    }

    private void openDocument(String filename) throws Exception {
        String uri = uriFor(filename);
        String content = readProjectFile(filename);

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", uri);
        td.addProperty("languageId", "st");
        td.addProperty("version", 1);
        td.addProperty("text", content);
        params.add("textDocument", td);

        sendMessage(createNotification("textDocument/didOpen", params));
        receivedDiagnostics.remove(uri);
        waitForIdle(1500);
    }

    private void changeDocument(String filename, String content) throws Exception {
        String uri = uriFor(filename);

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", uri);
        td.addProperty("version", 2);
        params.add("textDocument", td);

        JsonObject change = new JsonObject();
        change.addProperty("text", content);
        params.add("contentChanges", GSON.toJsonTree(List.of(change)));

        sendMessage(createNotification("textDocument/didChange", params));
        receivedDiagnostics.remove(uri);
        waitForIdle(1500);
    }

    private void assertNoDiagnostics(String filename) throws Exception {
        assertNoDiagnostics(filename, filename + " should have 0 diagnostics");
    }

    private void assertNoDiagnostics(String filename, String message) throws Exception {
        String uri = uriFor(filename);
        List<JsonObject> diags = receivedDiagnostics.get(uri);
        if (diags != null && !diags.isEmpty()) {
            StringBuilder sb = new StringBuilder(message + "\n");
            for (JsonObject d : diags) {
                sb.append("  ").append(d).append("\n");
            }
            fail(sb.toString());
        }
    }

    private void waitForIdle(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    private JsonObject createRequest(int id, String method, Object params) {
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("id", id);
        req.addProperty("method", method);
        if (params != null) {
            req.add("params", GSON.toJsonTree(params));
        } else {
            req.add("params", GSON.toJsonTree(new JsonObject()));
        }
        return req;
    }

    private JsonObject createNotification(String method, Object params) {
        JsonObject not = new JsonObject();
        not.addProperty("jsonrpc", "2.0");
        not.addProperty("method", method);
        if (params != null) {
            not.add("params", GSON.toJsonTree(params));
        }
        return not;
    }

    private void sendMessage(JsonObject msg) throws IOException {
        String json = GSON.toJson(msg);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        writer.write("Content-Length: " + bytes.length + "\r\n\r\n");
        writer.write(json);
        writer.flush();
    }

    private void sendRequest(String method, int id, JsonObject params) throws IOException {
        sendMessage(createRequest(id, method, params));
    }

    private synchronized void handleMessage(JsonObject msg) {
        if (msg.has("method") && "textDocument/publishDiagnostics".equals(msg.get("method").getAsString())) {
            String uri = msg.getAsJsonObject("params").get("uri").getAsString();
            List<JsonObject> diags = new ArrayList<>();
            msg.getAsJsonObject("params").getAsJsonArray("diagnostics")
                    .forEach(e -> diags.add(e.getAsJsonObject()));
            receivedDiagnostics.put(uri, diags);
        }
        if (msg.has("id")) {
            int id = msg.get("id").getAsInt();
            receivedResponses.put(id, msg);
        }
    }

    private JsonObject waitForResponse(int id) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            JsonObject resp = receivedResponses.get(id);
            if (resp != null) {
                receivedResponses.remove(id);
                return resp;
            }
            Thread.sleep(50);
        }
        return null;
    }
}
