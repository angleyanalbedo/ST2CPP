# ST2C LSP 快速验证脚本
# 用法: pwsh -File test\lsp_test.ps1

$ErrorActionPreference = 'Stop'
$P = 0; $F = 0
function ok($n) { $global:P++; Write-Host "  PASS  $n" -F Green }
function nok($n,$m) { $global:F++; Write-Host "  FAIL  $n --- $m" -F Red }

$jar = "D:\source\Project\ST2C-master\java\target\st2c-jar-with-dependencies.jar"
if (-not (Test-Path $jar)) { Write-Host "JAR not found: $jar" -F Red; exit 1 }

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-cp `"$jar`" com.st2c.lsp.ST2CLanguageServer"
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)
$proc.PriorityClass = [System.Diagnostics.ProcessPriorityClass]::Normal

function send($obj) {
    $json = ConvertTo-Json $obj -Depth 10 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $header = "Content-Length: $($bytes.Length)`r`n`r`n"
    $proc.StandardInput.Write($header)
    $proc.StandardInput.Write($json)
    $proc.StandardInput.Flush()
}

function recv($id, $timeoutMs = 3000) {
    $deadline = (Get-Date).AddMilliseconds($timeoutMs)
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if ($line -match 'Content-Length:\s+(\d+)') {
            $len = [int]$Matches[1]
            $proc.StandardOutput.ReadLine() | Out-Null  # blank line
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
            $body = -join $buf
            $msg = $body | ConvertFrom-Json
            if ($msg.id -eq $id) { return $msg }
        }
    }
    return $null
}

# 1. Initialize
Write-Host "[1] Initialize" -F Cyan
send @{ jsonrpc = "2.0"; id = 1; method = "initialize"; params = @{ processId = 9999; capabilities = @{} } }
$resp = recv 1
if ($resp -and $resp.result) {
    $caps = $resp.result.capabilities
    if ($caps.hoverProvider) { ok "hover=supported" } else { nok "hover" "not supported" }
    if ($caps.completionProvider) { ok "completion=supported" } else { nok "completion" "not supported" }
} else { nok "initialize" ($resp | ConvertTo-Json) }

# 2. Initialized notification
send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }

# 3. DidOpen with ST code
Write-Host "[2] DidOpen + Diagnostics" -F Cyan
send @{
    jsonrpc = "2.0"; method = "textDocument/didOpen"
    params = @{
        textDocument = @{
            uri = "file:///test.st"; languageId = "st"; version = 1
            text = @"
PROGRAM MAIN
  VAR
    Counter : INT := 42;
    Value   : REAL := 3.14;
  END_VAR
  Counter := Counter + 1;
  Value := Value * 2.0;
END_PROGRAM
"@
        }
    }
}
# 等待诊断结果
Start-Sleep 1
ok "didOpen sent"

# 4. Hover on "INT"
Write-Host "[3] Hover" -F Cyan
send @{
    jsonrpc = "2.0"; id = 2; method = "textDocument/hover"
    params = @{
        textDocument = @{ uri = "file:///test.st" }
        position = @{ line = 2; character = 15 }
    }
}
$resp = recv 2
if ($resp -and $resp.result -and $resp.result.contents) {
    $val = $resp.result.contents.value
    if ($val) { ok "hover: $($val.Substring(0, [Math]::Min(60, $val.Length)))" }
    else { nok "hover" "empty content" }
} else { nok "hover" ($resp | ConvertTo-Json) }

# 5. Completion
Write-Host "[4] Completion" -F Cyan
send @{
    jsonrpc = "2.0"; id = 3; method = "textDocument/completion"
    params = @{
        textDocument = @{ uri = "file:///test.st" }
        position = @{ line = 0; character = 0 }
    }
}
$resp = recv 3
if ($resp -and $resp.result -and $resp.result.items) {
    $count = $resp.result.items.Count
    $first = $resp.result.items[0].label
    ok "completion: $count items (first: $first)"
} else { nok "completion" ($resp | ConvertTo-Json) }

# 6. Shutdown
Write-Host "[5] Shutdown" -F Cyan
send @{ jsonrpc = "2.0"; id = 4; method = "shutdown"; params = $null }
$resp = recv 4
if ($resp) { ok "shutdown OK" } else { nok "shutdown" "no response" }
send @{ jsonrpc = "2.0"; method = "exit"; params = @{} }

Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
exit $F
