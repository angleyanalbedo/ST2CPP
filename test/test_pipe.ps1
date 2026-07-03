$ErrorActionPreference = 'Stop'
$P = 0; $F = 0
function ok($n) { $global:P++; Write-Host "  PASS  $n" -F Green }
function nok($n,$m) { $global:F++; Write-Host "  FAIL  $n --- $m" -F Red }

$jar = "D:\source\Project\ST2C-master\java\target\st2c-jar-with-dependencies.jar"
$root = "D:/source/Project/ST2C-master/examples/projects/robot_arm"

# Write test input to a file
$ioConfig = Get-Content -Raw "$($root -replace '/','\')/io_config.st"
$msg1 = @{
    jsonrpc = "2.0"; id = 1; method = "initialize"
    params = @{ processId = 9999; capabilities = @{} }
}
$msg2 = @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }
$msg3 = @{
    jsonrpc = "2.0"; method = "textDocument/didOpen"
    params = @{ textDocument = @{ uri = "file:///$root/io_config.st"; languageId = "st"; version = 1; text = $ioConfig } }
}
$msg4 = @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
$msg5 = @{ jsonrpc = "2.0"; method = "exit"; params = @{} }
$msgs = @($msg1, $msg2, $msg3, $msg4, $msg5)

$inputData = ""
foreach ($msg in $msgs) {
    $json = $msg | ConvertTo-Json -Depth 10 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $inputData += "Content-Length: $($bytes.Length)`r`n`r`n$json"
}
Set-Content D:\source\Project\ST2C-master\test\tmp_lsp_input.txt $inputData -NoNewline

# Run LSP with the input file, output to file
$proc = Start-Process -NoNewWindow -PassThru -FilePath "java" `
    -ArgumentList "-cp", "`"$jar`"", "com.st2c.lsp.ST2CLanguageServer" `
    -RedirectStandardInput "D:\source\Project\ST2C-master\test\tmp_lsp_input.txt" `
    -RedirectStandardOutput "D:\source\Project\ST2C-master\test\tmp_lsp_output.txt" `
    -RedirectStandardError "D:\source\Project\ST2C-master\test\tmp_lsp_err.txt"
Start-Sleep 10
$proc.Kill()

$output = Get-Content "D:\source\Project\ST2C-master\test\tmp_lsp_output.txt" -Raw
$err = Get-Content "D:\source\Project\ST2C-master\test\tmp_lsp_err.txt" -Raw

if ($output) { Write-Host "Output ($($output.Length) chars):" -F Cyan; Write-Host $output.Substring(0, [Math]::Min(2000, $output.Length)) -F White }
if ($err) { Write-Host "`nStderr ($($err.Length) chars):" -F Yellow; Write-Host $err -F DarkYellow }
