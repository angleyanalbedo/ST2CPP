import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import PLCTranslator.FlatCodeGenerator;
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
 * ST2C++ 编译器入口（Flat 后端）
 */
public class Main {

    private static final String VERSION = "1.1.0";

    public static void main(String[] args) throws Exception {
        // 默认参数
        String backend = "flat";
        String inputFile = "pou.st";
        String outputFile = "main.cpp";
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

        // 选择代码生成器（仅 Flat 后端）
        FlatCodeGenerator codeGen = new FlatCodeGenerator();
        // 从输入文件名推导 fileId（如 "examples/test.st" → "test"）
        String fileName = new File(inputFile).getName();
        String fileId = fileName.endsWith(".st") ? fileName.substring(0, fileName.length() - 3) : fileName;
        codeGen.setFileId(fileId);

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
        System.out.println("\n" + codeGen.getOffsetDefinitions());

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
        System.out.println("  --backend <mode>     Reserved, only 'flat' is supported");
        System.out.println("  --input <file>       Input ST source file ");
        System.out.println("  --output <file>      Output C++ file ");
        System.out.println("  --verbose            Print detailed translation statistics");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java Main --input program.st --output out.cpp");
    }
}
