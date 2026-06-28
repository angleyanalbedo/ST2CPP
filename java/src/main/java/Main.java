import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCTranslator.CompilerConfig;
import PLCTranslator.GvlContext;
import PLCTranslator.PLCTranslatorNew;
import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * ST2C++ 编译器入口（Flat 后端）
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CompilerConfig cfg = new CompilerConfig();
        if (!cfg.parse(args)) {
            if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
                cfg.printHelp();
            }
            return;
        }
        cfg.validate();

        PLCSymbolAndScope.CompilerState.reset();
        long startTime = System.currentTimeMillis();

        GvlContext gvlCtx = new GvlContext();
        gvlCtx.setFileId(cfg.resolvedFileId);

        if (cfg.verbose) {
            System.out.println("[Input]  " + String.join(", ", cfg.inputFiles));
            System.out.println("[Output] " + cfg.resolvedOutputFile);
            System.out.println("[Cache]  " + (cfg.localCache ? "enabled" : "disabled"));
        }

        new Registrant().autoRegister();

        ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
        PLCVisitor plcVisitor = new PLCVisitor(property);
        PLCTranslatorNew translatorNew = new PLCTranslatorNew(property, gvlCtx);
        translatorNew.setLocalCache(cfg.localCache);

        StringBuilder fullCodeBuilder = new StringBuilder();
        int fileIndex = 0;

        // 标准库
        if (!cfg.noStdlib) {
            CharStream stdlibStream = loadStdlib(cfg.customStdlib);
            if (stdlibStream != null) {
                fullCodeBuilder.append(processStream(stdlibStream, plcVisitor, translatorNew, fileIndex++));
            }
        }

        // 用户源文件
        for (String inputPath : cfg.inputFiles) {
            CharStream charStream = CharStreams.fromFileName(inputPath);
            fullCodeBuilder.append(processStream(charStream, plcVisitor, translatorNew, fileIndex++));
        }

        // POU 注册
        fullCodeBuilder.append("\n");
        fullCodeBuilder.append(emitPOURegistration(cfg.resolvedFileId, gvlCtx));

        // 写文件
        String fullCode = fullCodeBuilder.toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cfg.resolvedOutputFile))) {
            writer.write(fullCode != null ? fullCode : "");
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (cfg.verbose) {
            int lines = fullCode != null ? fullCode.split("\n").length : 0;
            System.out.println("[Stats]  Generated " + lines + " lines of C++ code");
            System.out.println("[Time]   " + elapsed + " ms");
        }
        System.out.println("Translation completed: " + cfg.resolvedOutputFile);
    }

    private static CharStream loadStdlib(String customPath) {
        try {
            if (customPath != null) {
                File f = new File(customPath);
                if (!f.exists()) {
                    System.err.println("Error: stdlib file not found: " + customPath);
                    System.exit(1);
                }
                return CharStreams.fromFileName(customPath);
            }
            InputStream is = Main.class.getClassLoader().getResourceAsStream("iec_stdlib.st");
            if (is != null) {
                return CharStreams.fromStream(is, StandardCharsets.UTF_8);
            }
            System.err.println("Warning: built-in stdlib not found, skipping");
        } catch (Exception e) {
            System.err.println("Error loading stdlib: " + e.getMessage());
        }
        return null;
    }

    private static String processStream(CharStream charStream, PLCVisitor plcVisitor,
                                         PLCTranslatorNew translatorNew, int fileIndex) {
        PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
        ParseTree tree = parser.startpoint();
        plcVisitor.visit(tree);
        translatorNew.setEmitHeader(fileIndex == 0);
        translatorNew.setEmitPOURegistration(false);
        String code = translatorNew.visit(tree);
        return (code != null ? (fileIndex > 0 ? "\n" : "") + code : "");
    }

    private static String emitPOURegistration(String fileId, GvlContext gvlCtx) {
        StringBuilder sb = new StringBuilder();
        java.util.List<String> progNames = gvlCtx.getProgramNames();
        if (progNames == null || progNames.isEmpty()) return "";

        sb.append("// ─── Auto-generated POU Registration (").append(fileId).append(") ───\n");
        sb.append("void registerPOU_").append(fileId).append("(POURegistry& reg) {\n");
        for (String name : progNames) {
            String mangled = fileId.isEmpty() ? name : fileId + "_" + name;
            sb.append("    POUCallbacks cbs;\n");
            sb.append("    cbs.init = PROGRAM_").append(mangled).append("_init;\n");
            sb.append("    cbs.cyclic = PROGRAM_").append(mangled).append("_cyclic;\n");
            sb.append("    cbs.pre = PROGRAM_").append(mangled).append("_pre;\n");
            sb.append("    cbs.post = PROGRAM_").append(mangled).append("_post;\n");
            sb.append("    reg.add(\"").append(name).append("\", cbs);\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
