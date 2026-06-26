package com.st2c.lsp.analyzers;

import java.util.Map;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;

public class HoverAnalyzer {
    private static final Map<String, String> KEYWORD_DOCS = Map.ofEntries(
        Map.entry("PROGRAM", "定义一个 PLC 程序块\n\n```st\nPROGRAM ProgramName\n  VAR\n    ...\n  END_VAR\n  ...\nEND_PROGRAM\n```"),
        Map.entry("FUNCTION", "定义一个函数\n\n```st\nFUNCTION FuncName : ReturnType\n  VAR_INPUT\n    ...\n  END_VAR\n  ...\nEND_FUNCTION\n```"),
        Map.entry("FUNCTION_BLOCK", "定义一个功能块\n\n```st\nFUNCTION_BLOCK FBName\n  VAR_INPUT\n    ...\n  END_VAR\n  VAR_OUTPUT\n    ...\n  END_VAR\n  ...\nEND_FUNCTION_BLOCK\n```"),
        Map.entry("VAR", "变量声明块\n\n```st\nVAR\n  variable_name : TYPE;\nEND_VAR\n```"),
        Map.entry("IF", "条件语句\n\n```st\nIF condition THEN\n  ...\nELSIF condition THEN\n  ...\nELSE\n  ...\nEND_IF\n```"),
        Map.entry("FOR", "循环语句\n\n```st\nFOR i := 0 TO 10 BY 1 DO\n  ...\nEND_FOR\n```"),
        Map.entry("WHILE", "循环语句\n\n```st\nWHILE condition DO\n  ...\nEND_WHILE\n```"),
        Map.entry("REPEAT", "循环语句\n\n```st\nREPEAT\n  ...\nUNTIL condition\nEND_REPEAT\n```"),
        Map.entry("CASE", "选择语句\n\n```st\nCASE expression OF\n  0: ...\n  1: ...\nELSE\n  ...\nEND_CASE\n```")
    );

    private static final Map<String, String> TYPE_DOCS = Map.ofEntries(
        Map.entry("INT", "16位有符号整数 (-32768 到 32767)"),
        Map.entry("DINT", "32位有符号整数"),
        Map.entry("LINT", "64位有符号整数"),
        Map.entry("SINT", "8位有符号整数 (-128 到 127)"),
        Map.entry("UINT", "16位无符号整数"),
        Map.entry("UDINT", "32位无符号整数"),
        Map.entry("ULINT", "64位无符号整数"),
        Map.entry("USINT", "8位无符号整数"),
        Map.entry("REAL", "32位浮点数"),
        Map.entry("LREAL", "64位浮点数"),
        Map.entry("BOOL", "布尔值 (TRUE/FALSE)"),
        Map.entry("STRING", "字符串类型"),
        Map.entry("TIME", "时间类型")
    );

    private static final Map<String, String> FUNCTION_DOCS = Map.ofEntries(
        Map.entry("ABS", "`ABS(x : ANY_NUM) : ANY_NUM`\n\n返回 x 的绝对值"),
        Map.entry("SQRT", "`SQRT(x : ANY_REAL) : ANY_REAL`\n\n返回 x 的平方根"),
        Map.entry("SIN", "`SIN(x : ANY_REAL) : ANY_REAL`\n\n返回 x 的正弦值"),
        Map.entry("COS", "`COS(x : ANY_REAL) : ANY_REAL`\n\n返回 x 的余弦值"),
        Map.entry("TAN", "`TAN(x : ANY_REAL) : ANY_REAL`\n\n返回 x 的正切值"),
        Map.entry("LEN", "`LEN(s : STRING) : INT`\n\n返回字符串长度"),
        Map.entry("CONCAT", "`CONCAT(s1 : STRING, s2 : STRING) : STRING`\n\n连接两个字符串"),
        Map.entry("MAX", "`MAX(a : ANY, b : ANY) : ANY`\n\n返回较大值"),
        Map.entry("MIN", "`MIN(a : ANY, b : ANY) : ANY`\n\n返回较小值")
    );

    private static final Map<String, String> FUNCTION_BLOCK_DOCS = Map.ofEntries(
        Map.entry("TON", "接通延时定时器\n\n```st\ntimer : TON;\ntimer(IN := input, PT := T#1s);\n```"),
        Map.entry("TOF", "断开延时定时器\n\n```st\ntimer : TOF;\ntimer(IN := input, PT := T#1s);\n```"),
        Map.entry("TP", "脉冲定时器\n\n```st\ntimer : TP;\ntimer(IN := input, PT := T#1s);\n```"),
        Map.entry("CTU", "加计数器\n\n```st\ncounter : CTU;\ncounter(CU := input, R := reset, PV := 10);\n```"),
        Map.entry("CTD", "减计数器\n\n```st\ncounter : CTD;\ncounter(CD := input, LD := load, PV := 10);\n```")
    );

    public Hover getHover(String uri, int line, int character) {
        String word = getWordAtPosition(uri, line, character);
        if (word == null || word.isEmpty()) {
            return null;
        }

        String documentation = getKeyDocument(word);
        if (documentation == null) {
            return null;
        }

        MarkupContent content = new MarkupContent();
        content.setKind(MarkupKind.MARKDOWN);
        content.setValue(documentation);

        Hover hover = new Hover();
        hover.setContents(content);
        return hover;
    }

    private String getWordAtPosition(String uri, int line, int character) {
        return null;
    }

    private String getKeyDocument(String word) {
        String upper = word.toUpperCase();
        if (KEYWORD_DOCS.containsKey(upper)) {
            return KEYWORD_DOCS.get(upper);
        }
        if (TYPE_DOCS.containsKey(upper)) {
            return TYPE_DOCS.get(upper);
        }
        if (FUNCTION_DOCS.containsKey(upper)) {
            return FUNCTION_DOCS.get(upper);
        }
        if (FUNCTION_BLOCK_DOCS.containsKey(upper)) {
            return FUNCTION_BLOCK_DOCS.get(upper);
        }
        return null;
    }
}
