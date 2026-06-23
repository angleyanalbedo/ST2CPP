import PLCSymbolAndScope.PLCScope.PLCScope;
import PLCSymbolAndScope.PLCSymbolTables.PLCSymbolTable;
import PLCSymbolAndScope.PLCSymbolTables.PLCTotalSymbolTable;
import PLCSymbolAndScope.PLCSymbols.PLCRefDeclSymbol;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
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
        //注册访问策略
        new Registrant().autoRegister();

        //读取文件获得语法树
        CharStream charStream = CharStreams.fromFileName("src/main/resources/input/input.st");
        PLCSTPARSERLexer plcLexer = new PLCSTPARSERLexer(charStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(plcLexer);
        PLCSTPARSERParser helloParser = new PLCSTPARSERParser(commonTokenStream);

        ParseTree parseTree = helloParser.startpoint();

        ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
        PLCVisitor plcVisitor = new PLCVisitor(property);

        plcVisitor.visit(parseTree);

        PLCTranslatorNew translatorNew = new PLCTranslatorNew(property);

        translatorNew.visit(parseTree);

        closeWriter();
    }

}
