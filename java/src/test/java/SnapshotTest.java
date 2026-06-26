import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.junit.BeforeClass;
import org.junit.Test;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCSymbolAndScope.PLCScopeStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SnapshotTest {

    private static final File SNAPSHOT_DIR = new File("src/test/resources/snapshots");
    private static final boolean UPDATE_MODE = "true".equalsIgnoreCase(
        System.getProperty("updateSnapshots", "false"));

    @BeforeClass
    public static void setup() {
        try { new Registrant().autoRegister(); } catch (Exception e) { /* ignore */ }
    }

    @Test
    public void test() throws Exception {
        File[] stFiles = SNAPSHOT_DIR.listFiles((dir, name) -> name.endsWith(".st"));
        assertTrue("No .st files found in " + SNAPSHOT_DIR, stFiles != null && stFiles.length > 0);

        for (File stFile : stFiles) {
            String stem = stFile.getName().replace(".st", "");
            File goldenFile = new File(SNAPSHOT_DIR, stem + ".cpp");

            System.out.println("  Testing: " + stem);

            // Reset global state between files
            PLCScopeStack.reset();
            PLCScopeStack.stackInit();

            // Phase 1: Parse
            CharStream charStream = CharStreams.fromFileName(stFile.getAbsolutePath());
            PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());
            ParseTree tree = parser.startpoint();

            // Phase 2: Semantic analysis
            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);
            try {
                visitor.visit(tree);
            } catch (Exception e) {
                System.err.println("Semantic error for " + stem + ": " + e.getMessage());
                System.err.println("  arraySymbolMap keys: " + PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable.arraySymbolMap.keySet());
                System.err.println("  totalTypeMap keys: " + PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable.totalTypeMap.keySet());
                StackTraceElement[] st = e.getStackTrace();
                for (int i = 0; i < Math.min(st.length, 20); i++) {
                    System.err.println("  at " + st[i].getClassName() + "." + st[i].getMethodName()
                        + "(" + st[i].getFileName() + ":" + st[i].getLineNumber() + ")");
                }
                throw e;
            }

            // Phase 3: Code generation
            FlatCodeGenerator codeGen = new FlatCodeGenerator();
            codeGen.setFileId(stem);
            PLCTranslatorNew translator = new PLCTranslatorNew(property, codeGen);
            translator.setEmitHeader(true);
            translator.setEmitPOURegistration(true);
            String actual = translator.visit(tree);
            if (actual == null) {
                actual = "";
            }
            actual = actual.replace("\r\n", "\n");

            if (UPDATE_MODE) {
                Files.write(goldenFile.toPath(), actual.getBytes());
                System.out.println("    [UPDATED] " + goldenFile.getName());
            } else {
                assertTrue("Golden file not found: " + goldenFile
                    + "\nRun with -DupdateSnapshots=true to create it.", goldenFile.exists());
                String expected = new String(Files.readAllBytes(goldenFile.toPath()))
                    .replace("\r\n", "\n");
                assertEquals("Snapshot mismatch: " + stem, expected, actual);
                System.out.println("    [PASS] " + stem);
            }
        }
    }

    private static class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuntimeException("Parse error line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}
