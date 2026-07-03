package com.st2c.lsp.analyzers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import PLCSymbolAndScope.CompilerState;
import PLCSymbolAndScope.PLCSymbols.PLCSymbol;
import staticCheckVisitor.PLCVisitor;
import staticCheckVisitor.register.Registrant;

public class DiagnosticAnalyzer {
    private final Map<String, String> documents = new HashMap<>();
    private final Map<String, List<Diagnostic>> diagnosticsMap = new HashMap<>();
    private boolean strategiesRegistered = false;

    private synchronized void ensureStrategies() {
        if (strategiesRegistered) return;
        try {
            new Registrant().autoRegister();
            strategiesRegistered = true;
        } catch (Exception e) {
            System.err.println("[DiagnosticAnalyzer] Strategy registration: " + e.getMessage());
        }
    }

    public void updateDocument(String uri, String content) {
        synchronized (this) { documents.put(uri, content); }
        reanalyzeAll();
    }

    public void removeDocument(String uri) {
        synchronized (this) { documents.remove(uri); }
        reanalyzeAll();
    }

    public List<Diagnostic> getDiagnostics(String uri) {
        synchronized (this) { return diagnosticsMap.getOrDefault(uri, List.of()); }
    }

    private void reanalyzeAll() {
        ensureStrategies();
        synchronized (this) {
            diagnosticsMap.clear();
            if (documents.isEmpty()) return;

            // 重置全局状态，共享同一个 PLCVisitor 处理所有文件
            CompilerState.reset();
            ParseTreeProperty<ArrayList<PLCSymbol>> property = new ParseTreeProperty<>();
            PLCVisitor visitor = new PLCVisitor(property);

            for (var entry : documents.entrySet()) {
                String uri = entry.getKey();
                List<Diagnostic> diags = new ArrayList<>();
                try {
                    PLCSTPARSERParser parser = new PLCSTPARSERParser(
                        new CommonTokenStream(new PLCSTPARSERLexer(CharStreams.fromString(entry.getValue()))));
                    visitor.visit(parser.startpoint());
                } catch (Exception e) {
                    Diagnostic d = createDiagnostic(e);
                    if (d != null) diags.add(d);
                }
                diagnosticsMap.put(uri, diags);
            }
        }
    }

    private Diagnostic createDiagnostic(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) return null;
        Position start = new Position(0, 0), end = new Position(0, 0);
        if (e instanceof org.antlr.v4.runtime.RecognitionException) {
            var re = (org.antlr.v4.runtime.RecognitionException) e;
            start = new Position(re.getOffendingToken().getLine() - 1, re.getOffendingToken().getCharPositionInLine());
            end = new Position(start.getLine(), start.getCharacter() + re.getOffendingToken().getText().length());
        } else if (e instanceof PLCSemanticException) {
            var ctx = ((PLCSemanticException) e).getCtx();
            if (ctx != null) {
                start = new Position(ctx.getStart().getLine() - 1, ctx.getStart().getCharPositionInLine());
                end = new Position(ctx.getStop().getLine() - 1,
                    ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length());
            }
        }
        var d = new Diagnostic(new Range(start, end), msg);
        d.setSeverity(DiagnosticSeverity.Error);
        return d;
    }
}
