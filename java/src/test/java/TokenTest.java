import antlr4.PLCSTLEXER;
import antlr4.PLCSTPARSERParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import java.io.IOException;

public class TokenTest {
    public static void main(String[] args) throws IOException {
        String code = "FUNCTION AND : DWORD VAR_INPUT A : DWORD; B : DWORD; END_VAR END_FUNCTION\n";
        PLCSTLEXER lexer = new PLCSTLEXER(CharStreams.fromString(code));
        Token t = lexer.nextToken();
        while (t.getType() != Token.EOF) {
            String tokenName = PLCSTPARSERParser.VOCABULARY.getSymbolicName(t.getType());
            if (tokenName == null) tokenName = "T__" + t.getType();
            System.out.println("Token: " + tokenName + " = '" + t.getText() + "' at line " + t.getLine() + ":" + t.getCharPositionInLine());
            t = lexer.nextToken();
        }
    }
}
