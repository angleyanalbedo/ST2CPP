@echo off
java -cp D:\source\Project\ST2C-master\java\target\st2c-jar-with-dependencies.jar com.st2c.lsp.ST2CLanguageServer < %~dp0\lsp_test_input.json
