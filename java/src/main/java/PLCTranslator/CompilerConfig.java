package PLCTranslator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 编译器配置：CLI 参数解析 + 默认值 + 派生字段
 */
public class CompilerConfig {

    public static final String VERSION = "1.1.0";

    // ── 用户可指定 ──
    public String backend = "flat";
    public final List<String> inputFiles = new ArrayList<>();
    public String outputFile = null;
    public String outputDir = null;
    public String fileId = null;
    public boolean noStdlib = false;
    public String customStdlib = null;
    public boolean verbose = false;
    public boolean localCache = true;
    public boolean emitLineDirectives = false;
    public boolean generateDebug = false;

    // ── 派生字段（parse 后自动填充）──
    public String resolvedOutputFile;
    public String resolvedFileId;

    /**
     * 解析命令行参数，填充字段。遇到 -h/--help 返回 false 表示应退出。
     */
    public boolean parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h": case "--help":
                    return false;
                case "-v": case "--version":
                    System.out.println("ST2C++ version " + VERSION);
                    return false;
                case "--backend":
                    backend = nextArg(args, i++); break;
                case "--input":
                    inputFiles.add(nextArg(args, i++)); break;
                case "--output":
                    outputFile = nextArg(args, i++); break;
                case "--output-dir":
                    outputDir = nextArg(args, i++); break;
                case "--file-id":
                    fileId = nextArg(args, i++); break;
                case "--no-stdlib":
                    noStdlib = true; break;
                case "--stdlib":
                    customStdlib = nextArg(args, i++); break;
                case "--verbose":
                    verbose = true; break;
                case "--no-local-cache":
                    localCache = false; break;
                case "--emit-line-directives":
                    emitLineDirectives = true; break;
                case "--generate-debug":
                    generateDebug = true; break;
                default:
                    System.err.println("Unknown option: " + arg);
                    System.err.println("Use --help for usage information.");
                    System.exit(1);
            }
        }
        resolve();
        return true;
    }

    private String nextArg(String[] args, int i) {
        if (i + 1 < args.length) return args[++i];
        System.err.println("Error: " + args[i] + " requires a value");
        System.exit(1);
        return null;
    }

    /**
     * 解析 output 路径和 fileId（派生字段）
     */
    private void resolve() {
        if (inputFiles.isEmpty()) {
            inputFiles.add("pou.st");
        }

        if (outputDir != null) {
            String baseName = new File(inputFiles.get(0)).getName();
            String stem = inputFiles.size() == 1
                    ? (baseName.endsWith(".st") ? baseName.substring(0, baseName.length() - 3) : baseName)
                    : "main";
            resolvedOutputFile = new File(outputDir, stem + ".cpp").getPath();
        } else if (outputFile != null) {
            resolvedOutputFile = outputFile;
        } else {
            resolvedOutputFile = "main.cpp";
        }

        if (fileId != null) {
            resolvedFileId = fileId;
        } else if (inputFiles.size() == 1) {
            String inputName = new File(inputFiles.get(0)).getName();
            resolvedFileId = inputName.endsWith(".st") ? inputName.substring(0, inputName.length() - 3) : inputName;
        } else {
            resolvedFileId = "combined";
        }
    }

    /**
     * 校验输入文件存在性，自动创建输出目录
     */
    public void validate() {
        for (String inputPath : inputFiles) {
            if (!new File(inputPath).exists()) {
                System.err.println("Error: Input file not found: " + inputPath);
                System.exit(1);
            }
        }
        File outDir = new File(resolvedOutputFile).getParentFile();
        if (outDir != null && !outDir.exists()) {
            outDir.mkdirs();
        }
    }

    public void printHelp() {
        System.out.println("ST2C++ - Structured Text to C++ Translator");
        System.out.println("Version: " + VERSION);
        System.out.println();
        System.out.println("Usage: java Main [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help              Show this help message and exit");
        System.out.println("  -v, --version           Show version information and exit");
        System.out.println("  --backend <mode>        Reserved, only 'flat' is supported");
        System.out.println("  --input <file>          Input ST source file (.st)");
        System.out.println("  --output <file>         Output C++ file (.cpp)");
        System.out.println("  --output-dir <dir>      Auto-name output as <dir>/<stem>.cpp");
        System.out.println("  --file-id <id>          POU registration ID (default: output stem)");
        System.out.println("  --no-stdlib             Disable built-in standard library");
        System.out.println("  --stdlib <file>         Override built-in stdlib with custom file");
        System.out.println("  --no-local-cache        Disable cyclic local variable caching");
        System.out.println("                          Variables access gvl/io directly instead of locals");
        System.out.println("  --emit-line-directives  Emit #line directives for GDB source-level");
        System.out.println("                          debugging (map C++ back to ST source lines)");
        System.out.println("  --generate-debug        Generate debug metadata files:");
        System.out.println("                          debug_table.cpp + debug_map.json");
        System.out.println("  --verbose               Print detailed translation statistics");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar st2c.jar --input program.st --output out.cpp");
        System.out.println("  java -jar st2c.jar --input test.st --output-dir output/flat/build");
        System.out.println("  java -jar st2c.jar --input pou.st --no-stdlib");
        System.out.println("  java -jar st2c.jar --input plc.st --no-local-cache");
        System.out.println("  java -jar st2c.jar --input test.st --output-dir build --emit-line-directives");
    }
}
