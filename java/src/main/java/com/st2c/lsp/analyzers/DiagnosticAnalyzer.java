package com.st2c.lsp.analyzers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import antlr4.PLCSTPARSERLexer;
import antlr4.PLCSTPARSERParser;
import PLCException.PLCSemanticException;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;

public class DiagnosticAnalyzer {
    private final Map<String, List<Diagnostic>> diagnosticsMap = new ConcurrentHashMap<>();

    static {
        try {
            new Registrant().autoRegister();
        } catch (Exception e) {
            System.err.println("[DiagnosticAnalyzer] Failed to register strategies: " + e.getMessage());
        }
    }

    public void analyze(String uri, String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            CharStream charStream = CharStreams.fromString(content);
            PLCSTPARSERLexer lexer = new PLCSTPARSERLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PLCSTPARSERParser parser = new PLCSTPARSERParser(tokens);
            ParseTree tree = parser.startpoint();

            ParseTreeProperty<ArrayList<PLCSymbol>> properties = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(properties);
            visitor.visit(tree);
        } catch (Exception e) {
            Diagnostic diagnostic = createDiagnostic(e);
            if (diagnostic != null) {
                diagnostics.add(diagnostic);
            }
        }
        diagnosticsMap.put(uri, diagnostics);
    }

    private Diagnostic createDiagnostic(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            return null;
        }

        Position start = new Position(0, 0);
        Position end = new Position(0, 0);

        if (e instanceof org.antlr.v4.runtime.RecognitionException) {
            org.antlr.v4.runtime.RecognitionException re = (org.antlr.v4.runtime.RecognitionException) e;
            start = new Position(re.getOffendingToken().getLine() - 1,
                               re.getOffendingToken().getCharPositionInLine());
            end = new Position(start.getLine(),
                             start.getCharacter() + re.getOffendingToken().getText().length());
        } else if (e instanceof PLCSemanticException) {
            PLCSemanticException se = (PLCSemanticException) e;
            if (se.getCtx() != null) {
                var ctx = se.getCtx();
                start = new Position(ctx.getStart().getLine() - 1, ctx.getStart().getCharPositionInLine());
                end = new Position(ctx.getStop().getLine() - 1,
                    ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length());
            }
        }

        Range range = new Range(start, end);
        Diagnostic diagnostic = new Diagnostic(range, message);
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        return diagnostic;
    }

    public List<Diagnostic> getDiagnostics(String uri) {
        return diagnosticsMap.getOrDefault(uri, List.of());
    }
}
