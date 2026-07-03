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

function drainDiag($uriPattern, $timeoutMs = 3000) {
    $deadline = (Get-Date).AddMilliseconds($timeoutMs)
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if (-not $line) { Start-Sleep 0.1; continue }
        if ($line -match 'Content-Length:\s+(\d+)') {
            $len = [int]$Matches[1]
            $proc.StandardOutput.ReadLine() | Out-Null
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
            $body = -join $buf
            $msg = $body | ConvertFrom-Json
            if ($msg.method -eq 'textDocument/publishDiagnostics' -and $msg.params.uri -match $uriPattern) {
                return $msg.params.diagnostics
            }
        }
    }
    return $null
}

send @{ jsonrpc = "2.0"; id = 1; method = "initialize"; params = @{ processId = 9999; capabilities = @{} } }
Start-Sleep 0.5
$proc.StandardOutput.ReadLine() | Out-Null  # Content-Length for init response
$proc.StandardOutput.ReadLine() | Out-Null  # blank line
$proc.StandardOutput.Read($null, 0, 10000) | Out-Null  # skip init response body
send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }

# Open a file that references a type from another file in the same dir
$dir = "D:/source/Project/ST2C-master/examples/projects/syntax_tests"
$main = Get-Content "$($dir -replace 'D:', 'D:')/test_struct.st" -Raw

Write-Host "[1] Open test_struct.st (uses MY_POINT from .st)" -F Cyan
send @{
    jsonrpc = "2.0"; method = "textDocument/didOpen"
    params = @{
        textDocument = @{ uri = "file:///$dir/test_struct.st"; languageId = "st"; version = 1; text = $main }
    }
}
Start-Sleep 3
$diags = drainDiag "test_struct.st" 3000
if ($diags -and $diags.Count -gt 0) {
    $typeErr = $diags | Where-Object { $_.message -match 'can not find type' }
    if ($typeErr) { nok "test_struct.st: TYPE ERROR" ($typeErr | ConvertTo-Json) }
    else { ok "test_struct.st: $($diags.Count) diags (non-type errors)" }
} else {
    ok "test_struct.st: no errors" }

send @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
Start-Sleep 0.5; $proc.Kill()

Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
