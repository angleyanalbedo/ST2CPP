package com.st2c.lsp;

import com.st2c.lsp.analyzers.DiagnosticAnalyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * LSP 诊断分析器测试 — 验证跨文件类型引用。
 * 测试场景：io_config.st 引用 types.st 定义的 SERVO_STATE，
 * 期望：自动扫描同目录 .st 文件后，类型引用能被正确解析。
 */
public class LSPDiagnosticTest {
    public static void main(String[] args) throws IOException {
        String dir = "D:/source/Project/ST2C-master/examples/projects/robot_arm";
        String typesPath = dir + "/types.st";
        String ioConfigPath = dir + "/io_config.st";

        // 读取文件内容
        String typesContent = new String(Files.readAllBytes(Paths.get(typesPath)));
        String ioConfigContent = new String(Files.readAllBytes(Paths.get(ioConfigPath)));

        // 1. 先打开 io_config.st 测试自动扫描
        System.out.println("=== Test 1: Open io_config.st (auto-scan types.st) ===");
        DiagnosticAnalyzer da = new DiagnosticAnalyzer();
        da.updateDocument("file:///" + ioConfigPath.replace('\\', '/'), ioConfigContent);

        var ioDiags = da.getDiagnostics("file:///" + ioConfigPath.replace('\\', '/'));
        boolean hasTypeError = ioDiags.stream().anyMatch(d -> d.getMessage().contains("can not find type"));
        System.out.println("io_config.st diagnostics: " + ioDiags.size());
        ioDiags.forEach(d -> System.out.println("  " + d.getMessage()));
        if (hasTypeError) {
            System.out.println("FAIL: cross-file type resolution failed — SERVO_STATE not found");
        } else {
            System.out.println("PASS: SERVO_STATE resolved from types.st (auto-scan)");
        }

        // 2. 再打开 types.st 作为已打开文档
        System.out.println("\n=== Test 2: Open types.st as document ===");
        da.updateDocument("file:///" + typesPath.replace('\\', '/'), typesContent);
        ioDiags = da.getDiagnostics("file:///" + ioConfigPath.replace('\\', '/'));
        hasTypeError = ioDiags.stream().anyMatch(d -> d.getMessage().contains("can not find type"));
        ioDiags.forEach(d -> System.out.println("  " + d.getMessage()));
        if (hasTypeError) {
            System.out.println("FAIL: cross-file type resolution still failed");
        } else {
            System.out.println("PASS: SERVO_STATE resolved (types.st as document)");
        }

        System.out.println("\n=== Done ===");
    }
}
