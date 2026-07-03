$ErrorActionPreference = 'Continue'
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
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)

function send($o) {
    $j = $o | ConvertTo-Json -Depth 10 -Compress
    $b = [Text.Encoding]::UTF8.GetBytes($j)
    $proc.StandardInput.Write("Content-Length: $($b.Length)`r`n`r`n$j")
    $proc.StandardInput.Flush()
}
function drain($ms = 3000) {
    $deadline = (Get-Date).AddMilliseconds($ms)
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if (-not $line) { Start-Sleep 0.05; continue }
        if ($line -match '\d+') {
            $len = [int]$Matches[0]
            $proc.StandardOutput.ReadLine() | Out-Null
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
        }
    }
}

send @{ jsonrpc = "2.0"; id = 1; method = "initialize"; params = @{ processId = 9999; capabilities = @{} } }
drain(2000)
send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }
drain(1000)

$root = "D:/source/Project/ST2C-master/examples/projects/robot_arm"

Write-Host "[1] Open io_config.st (auto-load types.st)" -F Cyan
$code = Get-Content -Raw "$($root -replace '/','\')/io_config.st"
send @{ jsonrpc = "2.0"; method = "textDocument/didOpen"
params = @{ textDocument = @{ uri = "file:///$root/io_config.st"; languageId = "st"; version = 1; text = $code } } }
Start-Sleep 4
drain(1000)

# Read all remaining stdout to find diagnostics
$diagsFound = @()
$deadline = (Get-Date).AddMilliseconds(2000)
while ((Get-Date) -lt $deadline) {
    $line = $proc.StandardOutput.ReadLine()
    if (-not $line) { Start-Sleep 0.05; continue }
    if ($line -match 'Content-Length:\s+(\d+)') {
        $len = [int]$Matches[1]
        $proc.StandardOutput.ReadLine() | Out-Null
        $buf = New-Object char[] $len
        $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
        $msg = (-join $buf) | ConvertFrom-Json
        if ($msg.method -eq 'textDocument/publishDiagnostics') {
            $diagsFound += [PSCustomObject]@{
                uri = $msg.params.uri
                count = $msg.params.diagnostics.Count
                typeErrors = @($msg.params.diagnostics | Where-Object { $_.message -match 'can not find type' })
            }
        }
    }
}

if ($diagsFound.Count -eq 0) {
    ok "io_config.st: no diagnostics (types resolved)"
} else {
    $ioDiag = $diagsFound | Where-Object { $_.uri -match 'io_config' }
    if ($ioDiag) {
        if ($ioDiag.typeErrors.Count -gt 0) {
            nok "io_config.st: $($ioDiag.typeErrors.Count) TYPE errors" $ioDiag.typeErrors[0].message
        } else { ok "io_config.st: $($ioDiag.count) diags (no type errors)" }
    }
}

send @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
Start-Sleep 1
$proc.Kill()
Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
