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
    $proc.StandardInput.Write($json); $proc.StandardInput.Flush()
}
function readMsg {
    $line = $proc.StandardOutput.ReadLine()
    if (-not $line) { return $null }
    if ($line -match 'Content-Length:\s+(\d+)') {
        $len = [int]$Matches[1]
        $proc.StandardOutput.ReadLine() | Out-Null
        $buf = New-Object char[] $len
        $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
        return (-join $buf) | ConvertFrom-Json
    }
    return $null
}

send @{ jsonrpc = "2.0"; id = 1; method = "initialize"; params = @{ processId = 9999; capabilities = @{} } }
readMsg | Out-Null; send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }
Start-Sleep 1

# Open io_config.st (references SERVO_STATE from types.st)
$dir = "D:/source/Project/ST2C-master/examples/projects/robot_arm"
$code = Get-Content "$($dir -replace 'D:/','D:\')\io_config.st" -Raw
Write-Host "[1] Open io_config.st (auto-scan types.st from dir)" -F Cyan
send @{ jsonrpc = "2.0"; method = "textDocument/didOpen"
params = @{ textDocument = @{ uri = "file:///$dir/io_config.st"; languageId = "st"; version = 1; text = $code } } }
Start-Sleep 2
for ($i = 0; $i -lt 30; $i++) {
    $m = readMsg; if (-not $m) { break }
    if ($m.method -eq 'textDocument/publishDiagnostics') {
        $diag = $m.params.diagnostics
        $typeErr = $diag | Where-Object { $_.message -match 'can not find type' }
        if ($typeErr) { nok "io_config.st: $($typeErr.Count) type errors" }
        else { ok "io_config.st: $($diag.Count) diags (no type errors)" }
    }
}

send @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
Start-Sleep 0.5; $proc.Kill()
Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
