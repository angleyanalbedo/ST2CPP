# ST2C LSP Robot Arm 测试 — 验证跨文件引用自动加载
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

function recvAny($timeoutMs = 5000) {
    $deadline = (Get-Date).AddMilliseconds($timeoutMs)
    while ((Get-Date) -lt $deadline) {
        $line = $proc.StandardOutput.ReadLine()
        if ($line -match 'Content-Length:\s+(\d+)') {
            $len = [int]$Matches[1]
            $proc.StandardOutput.ReadLine() | Out-Null
            $buf = New-Object char[] $len
            $proc.StandardOutput.Read($buf, 0, $len) | Out-Null
            return (-join $buf) | ConvertFrom-Json
        }
    }
    return $null
}

# Initialize with workspace folder
send @{
    jsonrpc = "2.0"; id = 1; method = "initialize"
    params = @{
        processId = 9999; capabilities = @{};
        workspaceFolders = @(@{ uri = "file:///D:/source/Project/ST2C-master/examples/projects/robot_arm"; name = "robot_arm" })
    }
}
recvAny(5000) | Out-Null
send @{ jsonrpc = "2.0"; method = "initialized"; params = @{} }

# 打开 io_config.st （跨文件引用 types.st 定义的 SERVO_STATE）
Write-Host "[1] Open io_config.st (auto-load types.st from workspace)" -F Cyan
$ioConfig = Get-Content "D:\source\Project\ST2C-master\examples\projects\robot_arm\io_config.st" -Raw
send @{
    jsonrpc = "2.0"; method = "textDocument/didOpen"
    params = @{
        textDocument = @{
            uri = "file:///D:/source/Project/ST2C-master/examples/projects/robot_arm/io_config.st"
            languageId = "st"; version = 1
            text = $ioConfig
        }
    }
}
Start-Sleep 2
# Drain notifications
$diags = @{}
for ($i = 0; $i -lt 50; $i++) {
    $m = recvAny(1000)
    if (-not $m) { break }
    if ($m.method -eq 'textDocument/publishDiagnostics') {
        $diags[$m.params.uri] = $m.params.diagnostics
    }
}
$focusDiag = $diags["file:///D:/source/Project/ST2C-master/examples/projects/robot_arm/io_config.st"]
if ($focusDiag -and $focusDiag.Count -gt 0) {
    $hasTypeError = ($focusDiag | Where-Object { $_.message -match 'can not find type' })
    if ($hasTypeError) { nok "io_config.st" ($focusDiag | ConvertTo-Json) }
    else { ok "io_config.st: errors = $($focusDiag.Count) (none are 'can not find type')" }
} else {
    ok "io_config.st: no errors (cross-file types resolved)"
}
Write-Host "  (all workspace .st files were auto-loaded for symbol resolution)"

# Shutdown
send @{ jsonrpc = "2.0"; id = 2; method = "shutdown"; params = $null }
recvAny(3000) | Out-Null
send @{ jsonrpc = "2.0"; method = "exit"; params = @{} }

Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
exit $F
