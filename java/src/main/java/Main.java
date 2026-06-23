import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCRefDeclSymbol;
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
import staticCheckVisitor.factory.Factory;
import staticCheckVisitor.register.Registrant;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import static PLCSymbolAndScope.PLCScopeStack.*;
import static PLCTargetFileOutPut.TargetFileOutput.closeWriter;


public class Main {

    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        String backend = "oop";  // 默认 OOP 后端
        String inputFile = "src/main/resources/input/input.st";
        String outputFile = "src/main/resources/output/main.cpp";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--backend":
                    if (i + 1 < args.length) backend = args[++i];
                    break;
                case "--input":
                    if (i + 1 < args.length) inputFile = args[++i];
                    break;
                case "--output":
                    if (i + 1 < args.length) outputFile = args[++i];
                    break;
            }
        }

        // 选择代码生成器
        CodeGenerator codeGen;
        switch (backend.toLowerCase()) {
            case "flat":
                codeGen = new FlatCodeGenerator();
                System.out.println("Using Flat backend (rt_plc.h + rt_runtime.h)");
                break;
            case "oop":
            default:
                codeGen = new OOPCodeGenerator();
                System.out.println("Using OOP backend (PLC.h)");
                break;
        }

        //注册访问策略
        new Registrant().autoRegister();

        //读取文件获得语法树
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

        translatorNew.visit(parseTree);

        // 输出 Flat 后端的 GVL 偏移量信息
        if (codeGen instanceof FlatCodeGenerator) {
            FlatCodeGenerator flatGen = (FlatCodeGenerator) codeGen;
            System.out.println("\n" + flatGen.getOffsetDefinitions());
        }

        closeWriter();

        System.out.println("Translation completed: " + outputFile);
    }

}
