import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.PLCTranslatorNew;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OscatLibraryTest {

    private static final File ST_SOURCE_DIR = new File("../examples/st_source_code");
    private static final Set<String> ST2C_SUPPORTED = new HashSet<>(Arrays.asList(
        "PROGRAM", "FUNCTION", "VAR", "VAR_INPUT", "VAR_OUTPUT",
        "IF", "ELSIF", "ELSE", "END_IF",
        "FOR", "TO", "BY", "DO", "END_FOR",
        "WHILE", "END_WHILE", "REPEAT", "END_REPEAT",
        "CASE", "END_CASE", "RETURN", "ASSERT", "PRINT"
    ));

    private final List<String> parseErrors   = new ArrayList<>();
    private final List<String> semanticErrors = new ArrayList<>();
    private final List<String> codegenErrors  = new ArrayList<>();
    private final List<String> passFiles      = new ArrayList<>();
    private int totalFiles = 0;

    // 统计文件使用的语法特性
    private final Map<String, Integer> syntaxUsage = new LinkedHashMap<>();

    @Before
    public void registerStrategies() {
        try { new Registrant().autoRegister(); } catch (Exception e) { /* ignore */ }
    }

    @Test
    public void testOscatLibrary() throws Exception {
        if (!ST_SOURCE_DIR.exists()) {
            System.out.println("[SKIP] st_source_code directory not found");
            return;
        }

        List<File> stFiles = collectStFiles(ST_SOURCE_DIR);
        // 采样测试：分期处理以控制内存 / 速度
        // 改为 0 则处理全部文件
        int maxFiles = 0;
        if (maxFiles > 0 && stFiles.size() > maxFiles) {
            stFiles = stFiles.subList(0, maxFiles);
        }
        totalFiles = stFiles.size();
        System.out.println("=== OSCAT Library Compiler Coverage Test ===");
        System.out.println("Files found: " + totalFiles);

        int funcCount = 0, fbCount = 0, progCount = 0;

        for (File f : stFiles) {
            String content = readFile(f);
            String stem = f.getName();

            // 统计 POU 类型
            if (content.contains("FUNCTION_BLOCK")) { fbCount++; scanSyntax(content, stem); continue; }
            if (content.matches("(?s)^\\s*FUNCTION\\s.*")) funcCount++;
            if (content.matches("(?s)^\\s*PROGRAM\\s.*")) progCount++;

            scanSyntax(content, stem);

            try {
                compileFile(f);
                passFiles.add(stem);
            } catch (ParseException e) {
                parseErrors.add(stem + " | " + truncate(e.getMessage(), 120));
            } catch (SemanticException e) {
                semanticErrors.add(stem + " | " + truncate(e.getMessage(), 120));
            } catch (CodegenException e) {
                codegenErrors.add(stem + " | " + truncate(e.getMessage(), 120));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                parseErrors.add(stem + " | " + truncate(msg, 120));
            }
        }

        printReport(funcCount, fbCount, progCount);
    }

    private void scanSyntax(String content, String stem) {
        check(content, "VAR_IN_OUT", "VAR_IN_OUT");
        check(content, "VAR_OUTPUT", "VAR_OUTPUT");
        check(content, "VAR_EXTERNAL", "VAR_EXTERNAL");
        check(content, "VAR_TEMP", "VAR_TEMP");
        check(content, "VAR_ACCESS", "VAR_ACCESS");
        check(content, "RETAIN", "RETAIN");
        check(content, "CONSTANT", "CONSTANT");
        check(content, "AT\\s+%", "AT % address binding");
        check(content, "T#", "TIME literal");
        check(content, "D#", "DATE literal");
        check(content, "TOD#", "TIME_OF_DAY literal");
        check(content, "DT#", "DATE_AND_TIME literal");
        check(content, "DINT#", "DINT typed literal");
        check(content, "INT#", "INT typed literal");
        check(content, "REAL#", "REAL typed literal");
        check(content, "REF_TO", "REF_TO");
        check(content, "\\bEN\\b.*\\bENO\\b", "EN/ENO pattern");
        check(content, "\\bSEL\\b", "SEL function");
        check(content, "\\bMUX\\b", "MUX function");
        check(content, "\\bLIMIT\\b", "LIMIT function");
        check(content, "\\bTO_INT\\b|\\bTO_REAL\\b|\\bTO_DINT\\b|\\bTO_BOOL\\b|\\bTO_STRING\\b", "type conversion");
        check(content, "\\bSR\\b|\\bRS\\b", "SR/RS flip-flop");
        check(content, "\\bEXIT\\b", "EXIT statement");
        check(content, "\\bELSE\\b", "ELSE");
        check(content, "\\bELSIF\\b", "ELSIF");
        check(content, "\\bFOR\\b", "FOR loop");
        check(content, "\\bWHILE\\b", "WHILE loop");
        check(content, "\\bREPEAT\\b", "REPEAT loop");
        check(content, "\\bCASE\\b", "CASE statement");
        check(content, "\\bRETURN\\b", "RETURN");
        check(content, "\\bARRAY\\b", "ARRAY type");
        check(content, "\\bSTRUCT\\b", "STRUCT type");
        check(content, "\\bTON\\b|\\bTOF\\b|\\bTP\\b", "timer FB (TON/TOF/TP)");
        check(content, "\\bCTU\\b|\\bCTD\\b|\\bCTUD\\b", "counter FB (CTU/CTD/CTUD)");
        check(content, "\\bR_TRIG\\b|\\bF_TRIG\\b", "edge detection FB");
        check(content, "\\bWORD\\b|\\bDWORD\\b|\\bBYTE\\b", "bit-string types");
        check(content, "\\bDATE\\b|\\bTOD\\b", "DATE/TOD types");
        check(content, "\\bDINT\\b", "DINT type");
        check(content, "\\bLINT\\b|\\bLREAL\\b", "64-bit types (LINT/LREAL)");
        check(content, "\\bSINT\\b|\\bUSINT\\b|\\bUINT\\b|\\bUDINT\\b|\\bULINT\\b", "small/unsigned types");
        check(content, "\\bSTRING\\b", "STRING type");
        check(content, "\\bBOOL\\b", "BOOL type");
        check(content, "\\bINT\\b", "INT type");
        check(content, "\\bREAL\\b", "REAL type");
    }

    private void check(String content, String regex, String feature) {
        if (content.matches("(?s).*" + regex + ".*")) {
            syntaxUsage.put(feature, syntaxUsage.getOrDefault(feature, 0) + 1);
        }
    }

    private void compileFile(File f) throws Exception {
        CharStream charStream = CharStreams.fromFileName(f.getAbsolutePath());

        // Phase 1: Parse
        PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener("parse"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener("parse"));
        ParseTree tree = parser.startpoint();

        // Phase 2: Semantic
        ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
        PLCVisitor visitor = new PLCVisitor(property);
        try {
            visitor.visit(tree);
        } catch (Exception e) {
            throw new SemanticException(unwrap(e));
        }

        // Phase 3: Codegen
        FlatCodeGenerator codeGen = new FlatCodeGenerator();
        PLCTranslatorNew translator = new PLCTranslatorNew(property, codeGen);
        translator.setEmitHeader(false);
        translator.setEmitPOURegistration(false);
        try {
            String code = translator.visit(tree);
            if (code == null) throw new CodegenException("codegen returned null");
        } catch (Exception e) {
            throw new CodegenException(unwrap(e));
        }
    }

    private void printReport(int funcCount, int fbCount, int progCount) {
        int pass = passFiles.size();
        int fail = totalFiles - pass;

        System.out.println();
        System.out.println("--- POU Type Distribution ---");
        System.out.printf("  FUNCTION:        %d%n", funcCount);
        System.out.printf("  FUNCTION_BLOCK:  %d (not yet supported)%n", fbCount);
        System.out.printf("  PROGRAM:         %d%n", progCount);
        System.out.printf("  Total:           %d%n", totalFiles);

        System.out.println();
        System.out.println("--- Compilation Results ---");
        System.out.printf("  PASS: %d (%.1f%%)%n", pass, pass * 100.0 / totalFiles);
        System.out.printf("  FAIL: %d (%.1f%%)%n", fail, fail * 100.0 / totalFiles);
        System.out.printf("    Parse errors:    %d%n", parseErrors.size());
        System.out.printf("    Semantic errors: %d%n", semanticErrors.size());
        System.out.printf("    Codegen errors:  %d%n", codegenErrors.size());

        System.out.println();
        System.out.println("--- Top Parse Errors ---");
        Map<String, Integer> parseCategories = categorize(parseErrors);
        printTop(parseCategories, 10);

        System.out.println();
        System.out.println("--- Top Semantic Errors ---");
        Map<String, Integer> semanticCategories = categorize(semanticErrors);
        printTop(semanticCategories, 10);

        System.out.println();
        System.out.println("--- Top Codegen Errors ---");
        Map<String, Integer> codegenCategories = categorize(codegenErrors);
        printTop(codegenCategories, 10);

        System.out.println();
        System.out.println("--- Missing Features (from failures) ---");
        printMissingFeatures();

        System.out.println();
        System.out.println("--- Syntax Feature Usage in 729 Files ---");
        System.out.println("(features used by OSCAT, ST2C support status)");
        printSyntaxUsage();
    }

    private Map<String, Integer> categorize(List<String> errors) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String e : errors) {
            String cat = classifyError(e);
            map.put(cat, map.getOrDefault(cat, 0) + 1);
        }
        return map;
    }

    private String classifyError(String error) {
        String lower = error.toLowerCase();
        if (lower.contains("mismatched input") && (lower.contains("function_block") || lower.contains("'function_block'")))
            return "FUNCTION_BLOCK keyword";
        if (lower.contains("mismatched input"))
            return "mismatched input (lowercase/unexpected token)";
        if (lower.contains("no viable alternative"))
            return "no viable alternative (unsupported syntax)";
        if (lower.contains("extraneous input"))
            return "extraneous input";
        if (lower.contains("missing"))
            return "missing token";
        if (lower.contains("duplication of name"))
            return "duplicate name";
        if (lower.contains("type mismatch"))
            return "type mismatch";
        if (lower.contains("cannot find") || lower.contains("not found") || lower.contains("undeclared"))
            return "symbol not found";
        if (lower.contains("return") && lower.contains("function"))
            return "missing RETURN";
        if (lower.contains("null") || lower.contains("nullpointer"))
            return "NPE / null";
        if (lower.contains("classcast"))
            return "ClassCastException";
        if (lower.contains("index") && lower.contains("out of bounds"))
            return "ArrayIndexOutOfBounds";
        return "other";
    }

    private void printTop(Map<String, Integer> map, int n) {
        map.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(n)
            .forEach(e -> System.out.printf("  %3dx  %s%n", e.getValue(), e.getKey()));
    }

    private void printMissingFeatures() {
        System.out.println("  FUNCTION_BLOCK      — 461 files (63% of total)");
        System.out.println("  lowercase IDs       — causes most parse errors");
        System.out.println("  VAR_IN_OUT / ENUM   — semantic/codegen gaps");
        System.out.println("  CONFIGURATION       — removed, not needed");
    }

    private void printSyntaxUsage() {
        Set<String> supported = new HashSet<>(Arrays.asList(
            "INT type", "REAL type", "BOOL type", "DINT type", "STRING type",
            "TIME literal", "FOR loop", "WHILE loop", "REPEAT loop", "CASE statement",
            "RETURN", "ARRAY type", "STRUCT type", "type conversion",
            "SEL function", "MUX function", "LIMIT function", "ELSE", "ELSIF"
        ));
        Set<String> partial = new HashSet<>(Arrays.asList(
            "RETAIN", "VAR_OUTPUT", "VAR_IN_OUT", "64-bit types (LINT/LREAL)",
            "small/unsigned types", "bit-string types", "DATE/TOD types",
            "DATE literal", "TIME_OF_DAY literal", "DATE_AND_TIME literal",
            "DINT typed literal", "INT typed literal", "REAL typed literal"
        ));
        syntaxUsage.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> {
                String status;
                if (supported.contains(e.getKey())) status = "  YES";
                else if (partial.contains(e.getKey())) status = " PART";
                else status = "   NO";
                System.out.printf("  %3dx  [%s] %s%n", e.getValue(), status, e.getKey());
            });
    }

    private static List<File> collectStFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (f.isDirectory()) result.addAll(collectStFiles(f));
            else if (f.getName().toUpperCase().endsWith(".ST")) result.add(f);
        }
        return result;
    }

    private static String readFile(File f) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(f.toPath()));
    }

    private static String truncate(String msg, int maxLen) {
        if (msg == null) return "null";
        return msg.length() > maxLen ? msg.substring(0, maxLen) + "..." : msg;
    }

    private static String unwrap(Exception e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    // ─── Custom exception types ───

    private static class ParseException extends Exception {
        ParseException(String msg) { super(msg); }
    }

    private static class SemanticException extends Exception {
        SemanticException(String msg) { super(msg); }
    }

    private static class CodegenException extends Exception {
        CodegenException(String msg) { super(msg); }
    }

    // ─── ANTLR error listener that throws ───

    private static class ThrowingErrorListener extends BaseErrorListener {
        private final String phase;
        ThrowingErrorListener(String phase) { this.phase = phase; }
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuntimeException(phase + " error line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}