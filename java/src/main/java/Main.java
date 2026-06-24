import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCTranslator.CodeGenerator;
import PLCTranslator.FlatCodeGenerator;
import PLCTranslator.OOPCodeGenerator;
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
import java.util.ArrayList;

/**
 * ST2C++ 编译器入口
 *
 * 支持多进程隔离：每个进程独立选择 OOP 或 Flat 后端模式。
 * 同一进程内多线程共享同一模式（通过 static codeGen）。
 */
public class Main {

    private static final String VERSION = "1.1.0";

    public static void main(String[] args) throws Exception {
        // 默认参数
        String backend = "oop";
        String inputFile = "src/main/resources/input/input.st";
        String outputFile = "src/main/resources/output/main.cpp";
        boolean verbose = false;

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h":
                case "--help":
                    printHelp();
                    return;
                case "-v":
                case "--version":
                    System.out.println("ST2C++ version " + VERSION);
                    return;
                case "--backend":
                    if (i + 1 < args.length) backend = args[++i];
                    break;
                case "--input":
                    if (i + 1 < args.length) inputFile = args[++i];
                    break;
                case "--output":
                    if (i + 1 < args.length) outputFile = args[++i];
                    break;
                case "--verbose":
                    verbose = true;
                    break;
                default:
                    System.err.println("Unknown option: " + arg);
                    System.err.println("Use --help for usage information.");
                    System.exit(1);
            }
        }

        // 验证后端模式
        backend = backend.toLowerCase();
        if (!backend.equals("oop") && !backend.equals("flat")) {
            System.err.println("Error: Unknown backend '" + backend + "'. Use 'oop' or 'flat'.");
            System.exit(1);
        }

        // 验证输入文件存在
        File input = new File(inputFile);
        if (!input.exists()) {
            System.err.println("Error: Input file not found: " + inputFile);
            System.exit(1);
        }

        // 自动创建输出目录
        File outputFileObj = new File(outputFile);
        File outputDir = outputFileObj.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        long startTime = System.currentTimeMillis();

        // 选择代码生成器
        CodeGenerator codeGen;
        switch (backend) {
            case "flat":
                codeGen = new FlatCodeGenerator();
                if (verbose) System.out.println("[Backend] Flat (rt_plc.h + rt_runtime.h)");
                break;
            case "oop":
            default:
                codeGen = new OOPCodeGenerator();
                if (verbose) System.out.println("[Backend] OOP (PLC.h)");
                break;
        }

        if (verbose) {
            System.out.println("[Input]  " + inputFile);
            System.out.println("[Output] " + outputFile);
        }

        // 注册访问策略
        new Registrant().autoRegister();

        // 读取文件获得语法树
        CharStream charStream = CharStreams.fromFileName(inputFile);
        PLCSTPARSERLexer plcLexer = new PLCSTPARSERLexer(charStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(plcLexer);
        PLCSTPARSERParser helloParser = new PLCSTPARSERParser(commonTokenStream);

        ParseTree parseTree = helloParser.startpoint();

        ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
        PLCVisitor plcVisitor = new PLCVisitor(property);

        plcVisitor.visit(parseTree);

        // 使用选择的后端创建翻译器
        PLCTranslatorNew translatorNew = new PLCTranslatorNew(property, codeGen);

        // visit 返回完整的代码字符串（不再在内部写文件）
        String fullCode = translatorNew.visit(parseTree);

        // 统一一次性写入文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(fullCode != null ? fullCode : "");
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // 输出 Flat 后端的 GVL 偏移量信息
        if (codeGen instanceof FlatCodeGenerator) {
            FlatCodeGenerator flatGen = (FlatCodeGenerator) codeGen;
            System.out.println("\n" + flatGen.getOffsetDefinitions());
        }

        if (verbose) {
            int codeLines = fullCode != null ? fullCode.split("\n").length : 0;
            System.out.println("[Stats]  Generated " + codeLines + " lines of C++ code");
            System.out.println("[Time]   " + elapsed + " ms");
        }

        System.out.println("Translation completed: " + outputFile);
    }

    private static void printHelp() {
        System.out.println("ST2C++ - Structured Text to C++ Translator");
        System.out.println("Version: " + VERSION);
        System.out.println();
        System.out.println("Usage: java Main [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help           Show this help message and exit");
        System.out.println("  -v, --version        Show version information and exit");
        System.out.println("  --backend <mode>     Code generator backend: oop | flat (default: oop)");
        System.out.println("  --input <file>       Input ST source file (default: src/main/resources/input/input.st)");
        System.out.println("  --output <file>      Output C++ file (default: src/main/resources/output/main.cpp)");
        System.out.println("  --verbose            Print detailed translation statistics");
        System.out.println();
        System.out.println("Backends:");
        System.out.println("  oop   - PLC_Value style, heap objects, debugging-friendly");
        System.out.println("  flat  - GVL offset style, zero heap, performance-oriented");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java Main --backend flat --input program.st --output out.cpp");
        System.out.println("  java Main --backend oop --verbose");
    }
}
