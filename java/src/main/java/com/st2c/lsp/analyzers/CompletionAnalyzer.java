package com.st2c.lsp.analyzers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;

public class CompletionAnalyzer {
    private static final List<String> KEYWORDS = List.of(
        "PROGRAM", "END_PROGRAM", "FUNCTION", "END_FUNCTION",
        "FUNCTION_BLOCK", "END_FUNCTION_BLOCK", "VAR", "END_VAR",
        "VAR_INPUT", "VAR_OUTPUT", "VAR_IN_OUT", "VAR_TEMP",
        "VAR_GLOBAL", "VAR_EXTERNAL", "RETAIN", "CONSTANT", "AT",
        "IF", "THEN", "ELSIF", "ELSE", "END_IF",
        "FOR", "TO", "BY", "DO", "END_FOR",
        "WHILE", "END_WHILE", "REPEAT", "UNTIL", "END_REPEAT",
        "CASE", "OF", "END_CASE", "RETURN",
        "TYPE", "END_TYPE", "STRUCT", "END_STRUCT", "ARRAY", "OF",
        "NAMESPACE", "END_NAMESPACE", "USING",
        "CONFIGURATION", "END_CONFIGURATION", "RESOURCE", "END_RESOURCE"
    );

    private static final List<String> TYPES = List.of(
        "INT", "DINT", "LINT", "SINT", "UINT", "UDINT", "ULINT", "USINT",
        "REAL", "LREAL", "BOOL", "STRING", "WSTRING", "CHAR", "WCHAR",
        "TIME", "LTIME", "DATE", "LDATE", "TIME_OF_DAY", "TOD", "LTOD",
        "DATE_AND_TIME", "DT", "LDT", "BYTE", "WORD", "DWORD", "LWORD"
    );

    private static final List<String> FUNCTIONS = List.of(
        "ABS", "SQRT", "LN", "LOG", "EXP", "EXPT", "TRUNC",
        "SIN", "COS", "TAN", "ASIN", "ACOS", "ATAN", "ATAN2",
        "ADD", "SUB", "MUL", "DIV", "MOD", "MOVE",
        "SHL", "SHR", "ROL", "ROR",
        "SEL", "MAX", "MIN", "LIMIT", "MUX",
        "GT", "GE", "EQ", "LE", "LT", "NE",
        "LEN", "LEFT", "RIGHT", "MID", "CONCAT", "INSERT", "DELETE", "REPLACE", "FIND"
    );

    private static final List<String> FUNCTION_BLOCKS = List.of(
        "TON", "TOF", "TP", "CTU", "CTD", "CTUD",
        "R_TRIG", "F_TRIG", "SR", "RS"
    );

    public CompletionList getCompletions(String uri, int line, int character) {
        List<CompletionItem> items = new ArrayList<>();

        for (String keyword : KEYWORDS) {
            items.add(createCompletionItem(keyword, CompletionItemKind.Keyword));
        }

        for (String type : TYPES) {
            items.add(createCompletionItem(type, CompletionItemKind.TypeParameter));
        }

        for (String func : FUNCTIONS) {
            items.add(createCompletionItem(func, CompletionItemKind.Function));
        }

        for (String fb : FUNCTION_BLOCKS) {
            items.add(createCompletionItem(fb, CompletionItemKind.Class));
        }

        CompletionList completionList = new CompletionList();
        completionList.setItems(items);
        return completionList;
    }

    private CompletionItem createCompletionItem(String label, CompletionItemKind kind) {
        CompletionItem item = new CompletionItem(label);
        item.setKind(kind);
        item.setDetail(kind.toString());
        return item;
    }
}
