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
import java.util.List;

/**
 * ST2C++ 编译器入口（Flat 后端）
 */
public class Main {

    private static final String VERSION = "1.1.0";

    public static void main(String[] args) throws Exception {
        // 默认参数
        String backend = "flat";
        List<String> inputFiles = new ArrayList<>();
        String outputFile = null;
        String outputDir = null;
        String fileId = null;
        String stdlibFile = null;
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
                    if (i + 1 < args.length) {
                        inputFiles.add(args[++i]);
                    } else {
                        System.err.println("Error: --input requires a file path");
                        System.exit(1);
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) outputFile = args[++i];
                    break;
                case "--output-dir":
                    if (i + 1 < args.length) outputDir = args[++i];
                    break;
                case "--file-id":
                    if (i + 1 < args.length) fileId = args[++i];
                    break;
                case "--stdlib":
                    if (i + 1 < args.length) stdlibFile = args[++i];
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

        // 如果没有指定输入文件，使用默认文件名
        if (inputFiles.isEmpty()) {
            inputFiles.add("pou.st");
        }

        // 解析输出文件路径（基于用户输入，不含 stdlib）
        if (outputDir != null) {
            String baseName = new File(inputFiles.get(0)).getName();
            String stem = inputFiles.size() == 1
                    ? (baseName.endsWith(".st") ? baseName.substring(0, baseName.length() - 3) : baseName)
                    : "main";
            outputFile = new File(outputDir, stem + ".cpp").getPath();
        } else if (outputFile == null) {
            outputFile = "main.cpp";
        }

        // 推导 fileId（从用户输入文件名派生，去掉 .st 后缀）
        if (fileId == null) {
            if (inputFiles.size() == 1) {
                String inputName = new File(inputFiles.get(0)).getName();
                fileId = inputName.endsWith(".st") ? inputName.substring(0, inputName.length() - 3) : inputName;
            } else {
                fileId = "combined";
            }
        }

        // 如果指定了 stdlib，插入到输入文件列表最前面
        if (stdlibFile != null) {
            File stdlib = new File(stdlibFile);
            if (!stdlib.exists()) {
                System.err.println("Error: stdlib file not found: " + stdlibFile);
                System.exit(1);
            }
            inputFiles.add(0, stdlibFile);
        }

        // 验证其余输入文件存在
        for (String inputPath : inputFiles) {
            File input = new File(inputPath);
            if (!input.exists()) {
                System.err.println("Error: Input file not found: " + inputPath);
                System.exit(1);
            }
        }

        // 自动创建输出目录
        File outputFileObj = new File(outputFile);
        File outputParentDir = outputFileObj.getParentFile();
        if (outputParentDir != null && !outputParentDir.exists()) {
            outputParentDir.mkdirs();
        }

        long startTime = System.currentTimeMillis();

        // 选择代码生成器（仅 Flat 后端）
        FlatCodeGenerator codeGen = new FlatCodeGenerator();
        codeGen.setFileId(fileId);

        if (verbose) {
            System.out.println("[Input]  " + String.join(", ", inputFiles));
            System.out.println("[Output] " + outputFile);
        }

        // 注册访问策略（只需一次）
        new Registrant().autoRegister();

        // 共享符号表和属性（跨所有输入文件）
        ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
        PLCVisitor plcVisitor = new PLCVisitor(property);
        PLCTranslatorNew translatorNew = new PLCTranslatorNew(property, codeGen);

        StringBuilder fullCodeBuilder = new StringBuilder();

        for (int fileIndex = 0; fileIndex < inputFiles.size(); fileIndex++) {
            String inputPath = inputFiles.get(fileIndex);
            if (fileIndex > 0) {
                fullCodeBuilder.append("\n");
            }

            CharStream charStream = CharStreams.fromFileName(inputPath);
            PLCSTPARSERLexer plcLexer = new PLCSTPARSERLexer(charStream);
            CommonTokenStream commonTokenStream = new CommonTokenStream(plcLexer);
            PLCSTPARSERParser helloParser = new PLCSTPARSERParser(commonTokenStream);

            ParseTree parseTree = helloParser.startpoint();

            plcVisitor.visit(parseTree);

            translatorNew.setEmitHeader(fileIndex == 0);
            translatorNew.setEmitPOURegistration(false);
            String fileCode = translatorNew.visit(parseTree);
            if (fileCode != null) {
                fullCodeBuilder.append(fileCode);
            }
        }

        // 统一一次性写入文件
        if (codeGen instanceof FlatCodeGenerator) {
            fullCodeBuilder.append("\n");
            fullCodeBuilder.append(((FlatCodeGenerator) codeGen).emitPOURegistration(codeGen.getFileId(), codeGen.getProgramNames()));
        }

        String fullCode = fullCodeBuilder.toString();
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
        System.out.println("  --input <file>       Input ST source file (.st)");
        System.out.println("  --output <file>      Output C++ file (.cpp)");
        System.out.println("  --output-dir <dir>   Auto-name output as <dir>/<stem>.cpp");
        System.out.println("  --file-id <id>       POU registration ID (default: output stem)");
        System.out.println("  --stdlib <file>      Standard library ST file (preloaded before user input)");
        System.out.println("  --verbose            Print detailed translation statistics");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar st2c.jar --input program.st --output out.cpp");
        System.out.println("  java -jar st2c.jar --input test.st --output-dir output/flat/build");
    }
}
