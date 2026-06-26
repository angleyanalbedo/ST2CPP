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
import PLCSymbolAndScope.PLCScopeStack;
import java.util.ArrayList;

public class DebugTest {
    public static void main(String[] args) throws Exception {
        new Registrant().autoRegister();
        PLCScopeStack.reset();
        PLCScopeStack.stackInit();

        CharStream charStream = CharStreams.fromFileName("src/test/resources/snapshots/test_arr_struct.st");
        PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
        ParseTree tree = parser.startpoint();
        System.out.println("Parse OK");

        ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
        PLCVisitor visitor = new PLCVisitor(property);
        try {
            visitor.visit(tree);
            System.out.println("Semantic OK");
        } catch (Exception e) {
            System.err.println("Semantic error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        FlatCodeGenerator codeGen = new FlatCodeGenerator();
        codeGen.setFileId("test_arr_struct");
        PLCTranslatorNew translator = new PLCTranslatorNew(property, codeGen);
        translator.setEmitHeader(true);
        translator.setEmitPOURegistration(true);
        String code = translator.visit(tree);
        System.out.println("Codegen OK, " + (code != null ? code.length() : 0) + " chars");
    }
}
