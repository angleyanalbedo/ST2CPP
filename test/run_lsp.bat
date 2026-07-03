@echo off
java -cp D:\source\Project\ST2C-master\java\target\st2c-jar-with-dependencies.jar com.st2c.lsp.ST2CLanguageServer < D:\source\Project\ST2C-master\test\tmp_lsp_input.txt > D:\source\Project\ST2C-master\test\tmp_lsp_output.txt 2> D:\source\Project\ST2C-master\test\tmp_lsp_err.txt
echo EXIT CODE: %ERRORLEVEL%
