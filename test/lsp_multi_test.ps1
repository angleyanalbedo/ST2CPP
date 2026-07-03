# ST2C LSP Multi-file 测试
# 验证跨文件类型引用（types.st 定义 → io_config.st 引用）
$ErrorActionPreference = 'Stop'
$P = 0; $F = 0
function ok($n) { $global:P++; Write-Host "  PASS  $n" -F Green }
function nok($n,$m) { $global:F++; Write-Host "  FAIL  $n --- $m" -F Red }

$jar = "D:\source\Project\ST2C-master\java\target\st2c-jar-with-dependencies.jar"
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-cp `"$jar`" com.st2c.lsp.ST2CLanguageServer"
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)

function send($obj) {
    $json = ConvertTo-Json $obj -Depth 10 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $proc.StandardInput.Write("Content-Length: $($bytes.Length)`r`n`r`n")
    $proc.StandardInput.Write($json)
    $proc.StandardInput.Flush()
}

function recv($id, $timeoutMs = 5000) {
    $deadline = (Get-Date).AddMilliseconds($timeoutMs)
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if ($line -match 'Content-Length:\s+(\d+)') {
            $len = [int]$Matches[1]
            $proc.StandardOutput.ReadLine() | Out-Null
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
            $body = -join $buf
            $msg = $body | ConvertFrom-Json
            if ($msg.id -eq $id) { return $msg }
        }
    }
    return $null
}

function getDiag {
    param([string]$uri)
    $diags = @()
    $deadline = (Get-Date).AddMilliseconds(1000)
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if ($line -match 'Content-Length:\s+(\d+)') {
            $len = [int]$Matches[1]
            $proc.StandardOutput.ReadLine() | Out-Null
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
            $body = -join $buf
            $msg = $body | ConvertFrom-Json
            if ($msg.method -eq 'textDocument/publishDiagnostics') {
                if ($msg.params.uri -eq $uri) {
                    return $msg.params.diagnostics
                }
            }
        }
    }
    return @()
}

# Initialize
send @{ jsonrpc = "2.0"; id = 1; method = "initialize"; params = @{ processId = 9999; capabilities = @{} } }
recv(1) | Out-Null
send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }

# 1. 先打开 types.st（定义 SERVO_STATE）
Write-Host "[1] Open types.st (define SERVO_STATE)" -F Cyan
send @{
    jsonrpc = "2.0"; method = "textDocument/didOpen"
    params = @{
        textDocument = @{
            uri = "file:///types.st"; languageId = "st"; version = 1
            text = @"
TYPE
    SERVO_STATE :
    STRUCT
        Enabled : BOOL;
        Position : INT;
        Velocity : INT;
    END_STRUCT;
END_TYPE
"@
        }
    }
}
$diags = getDiag "file:///types.st"
if ($diags.Count -eq 0) { ok "types.st: no errors" } else { nok "types.st" ($diags | ConvertTo-Json) }

# 2. 打开 io_config.st（引用 SERVO_STATE）
Write-Host "[2] Open io_config.st (use SERVO_STATE from types.st)" -F Cyan
send @{
    jsonrpc = "2.0"; method = "textDocument/didOpen"
    params = @{
        textDocument = @{
            uri = "file:///io_config.st"; languageId = "st"; version = 1
            text = @"
PROGRAM IO_CONFIG
    VAR
        SrvState : ARRAY[0..5] OF SERVO_STATE;
        Index : INT;
    END_VAR
    Index := 0;
END_PROGRAM
"@
        }
    }
}
Start-Sleep 1
$diags = getDiag "file:///io_config.st"
if ($diags.Count -eq 0) { ok "io_config.st: SERVO_STATE resolved correctly (no errors)" }
else { nok "io_config.st" ($diags | ConvertTo-Json) }

# 3. 再改 types.st，加一个错误（测试重分析）
Write-Host "[3] types.st changed -- remove SERVO_STATE" -F Cyan
send @{
    jsonrpc = "2.0"; method = "textDocument/didChange"
    params = @{
        textDocument = @{ uri = "file:///types.st"; version = 2 }
        contentChanges = @(@{ text = "TYPE END_TYPE" })
    }
}
Start-Sleep 1
$diags = getDiag "file:///io_config.st"
if ($diags.Count -gt 0) { ok "io_config.st: now correctly shows error (SERVO_STATE missing)" }
else { nok "io_config.st: should show error but didn't" }

# 4. Shutdown
send @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
recv(2) | Out-Null
send @{ jsonrpc = "2.0"; method = "exit"; params = @{} }

Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
exit $F
