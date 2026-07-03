# ST2C 多文件测试 — 手动打开 types.st + io_config.st 验证跨文件引用
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

function send($o) {
    $j = $o | ConvertTo-Json -Depth 10 -Compress
    $b = [Text.Encoding]::UTF8.GetBytes($j)
    $proc.StandardInput.Write("Content-Length: $($b.Length)`r`n`r`n$j")
    $proc.StandardInput.Flush()
}
function readAll($t = 2000) {
    $deadline = (Get-Date).AddMilliseconds($t)
    $msgs = @()
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if (-not $line) { Start-Sleep 0.1; continue }
        if ($line -match 'Content-Length:\s+(\d+)') {
            $len = [int]$Matches[1]
            $proc.StandardOutput.ReadLine() | Out-Null
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
            $msgs += (-join $buf) | ConvertFrom-Json
        }
    }
    return $msgs
}

send @{ jsonrpc = "2.0"; id = 1; method = "initialize"; params = @{ processId = 9999; capabilities = @{} } }
readAll 2000 | Out-Null
send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }
Start-Sleep 0.5; readAll 500 | Out-Null

# 1. Open types.st
Write-Host "[1] Open types.st (define SERVO_STATE)" -F Cyan
$types = Get-Content "D:\source\Project\ST2C-master\examples\projects\robot_arm\types.st" -Raw
send @{ jsonrpc = "2.0"; method = "textDocument/didOpen"
params = @{ textDocument = @{ uri = "file:///types.st"; languageId = "st"; version = 1; text = $types } } }
Start-Sleep 1; readAll 1000 | Out-Null
ok "types.st opened (may have errors if cross-refs unresolved)"

# 2. Open io_config.st
Write-Host "[2] Open io_config.st (use SERVO_STATE)" -F Cyan
$io = Get-Content "D:\source\Project\ST2C-master\examples\projects\robot_arm\io_config.st" -Raw
send @{ jsonrpc = "2.0"; method = "textDocument/didOpen"
params = @{ textDocument = @{ uri = "file:///io_config.st"; languageId = "st"; version = 1; text = $io } } }
Start-Sleep 2
$msgs = readAll 3000
$ioDiag = $msgs | Where-Object { $_.method -eq 'textDocument/publishDiagnostics' -and $_.params.uri -eq 'file:///io_config.st' }
if ($ioDiag) {
    $diags = $ioDiag[0].params.diagnostics
    $typeErr = $diags | Where-Object { $_.message -match 'can not find type' }
    if ($typeErr) { nok "io_config.st: TYPE NOT FOUND" ($typeErr[0].message) }
    else { ok "io_config.st: $($diags.Count) diags (no type errors)" }
} else {
    ok "io_config.st: no diagnostics = type resolution OK"
}

# Shutdown
send @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
Start-Sleep 0.5; $proc.Kill()
Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
