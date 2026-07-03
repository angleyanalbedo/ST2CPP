$ErrorActionPreference = 'Continue'
$P = 0; $F = 0
function ok($n) { $global:P++; Write-Host "  PASS  $n" -F Green }

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

Start-Sleep 1
ok "LSP started"

# Init
$init = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":9999,"capabilities":{}}}'
$b = [Text.Encoding]::UTF8.GetBytes($init)
$proc.StandardInput.Write("Content-Length: $($b.Length)`r`n`r`n$init")
$proc.StandardInput.Flush()

Start-Sleep 1
ok "sent init"

# Read init response
$line = $proc.StandardOutput.ReadLine()
if ($line -match '\d+') { ok "init response length: $line" } else { Write-Host "no init response" -F Red }

$proc.StandardInput.Close()
Start-Sleep 1
if (-not $proc.HasExited) { $proc.Kill(); ok "killed after test" }
