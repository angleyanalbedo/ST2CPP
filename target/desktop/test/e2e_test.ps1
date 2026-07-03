# ST2C Debug System End-to-End Test
# Usage: pwsh -File e2e_test.ps1 -Binary <path> -Map <debug_map.json> [-Host 127.0.0.1] [-Port 9090] [-CycleUs 5000]
param(
    [Parameter(Mandatory)] [string]$Binary,
    [Parameter(Mandatory)] [string]$Map,
    [string]$HostAddr = "127.0.0.1",
    [int]$Port = 9090,
    [int]$CycleUs = 5000
)

$ErrorActionPreference = 'Continue'
$P = 0; $F = 0
function ok($n)   { $global:P++; Write-Host "  PASS  $n" -F Green }
function nok($n,$m) { $global:F++; Write-Host "  FAIL  $n --- $m" -F Red }

# ─── parse debug_map.json (regex-based, avoids ConvertFrom-Json quirks) ───
if (-not (Test-Path $Map)) { Write-Host "FATAL: $Map not found" -F Red; exit 1 }
$raw = Get-Content $Map -Raw

function jsonGet($s, $key) {
    $pat1 = '"{0}"\s*:\s*"([^"]*)"' -f $key
    if ($s -match $pat1) { return $Matches[1] }
    $pat2 = '"{0}"\s*:\s*(-?\d+)' -f $key
    if ($s -match $pat2) { return [int]$Matches[1] }
    return $null
}

$allVars = @()
$re = '\{[^}]+\}'
foreach ($m in [regex]::Matches($raw, $re)) {
    $o = $m.Value
    $id = jsonGet $o 'id'
    if ($null -eq $id) { continue }
    $allVars += [PSCustomObject]@{
        id         = $id
        name       = (jsonGet $o 'name')
        storage    = (jsonGet $o 'storage')
        offset     = (jsonGet $o 'offset')
        bit_offset = (jsonGet $o 'bit_offset')
        type       = (jsonGet $o 'type')
        size       = (jsonGet $o 'size')
        count      = (jsonGet $o 'count')
        access     = (jsonGet $o 'access')
    }
}

$gvlS   = [System.Collections.Generic.List[object]]::new()
$inVars = [System.Collections.Generic.List[object]]::new()
$outVars= [System.Collections.Generic.List[object]]::new()
$arrs   = [System.Collections.Generic.List[object]]::new()
foreach ($v in $allVars) {
    if ($v.storage -eq 'GVL' -and $v.count -eq 1) { $gvlS.Add($v) }
    elseif ($v.storage -eq 'INPUT')  { $inVars.Add($v) }
    elseif ($v.storage -eq 'OUTPUT') { $outVars.Add($v) }
    if ($v.count -gt 1) { $arrs.Add($v) }
}

Write-Host "ST2C E2E: $($allVars.Count) vars ($($gvlS.Count) GVL, $($inVars.Count) IN, $($outVars.Count) OUT, $($arrs.Count) arr)" -F Cyan
if ($allVars.Count -eq 0) { Write-Host "FATAL: no variables parsed from $Map" -F Red; exit 1 }

# ─── start server ───
$svr = Start-Process -FilePath $Binary -ArgumentList "--cycle-us $CycleUs --diag-interval 99" -NoNewWindow -PassThru
Start-Sleep 2

try   { $c = [System.Net.Sockets.TcpClient]::new($HostAddr, $Port) }
catch { Write-Host "FATAL: cannot connect $HostAddr`:$Port" -F Red; $svr.Kill(); exit 1 }
$s = $c.GetStream(); $wr = [System.IO.StreamWriter]::new($s); $rd = [System.IO.StreamReader]::new($s)
$wr.AutoFlush = $true; $s.ReadTimeout = 3000

function recv {
    $deadline = (Get-Date).AddMilliseconds(500)
    while ((Get-Date) -lt $deadline) {
        try { $l = $rd.ReadLine(); if ($l -ne $null) { return $l } }
        catch { Start-Sleep 0.02; continue }
        Start-Sleep 0.02
    }
    return $null
}
function send($t) { $wr.WriteLine($t) }
function drain { for($i=0; $i -lt 5; $i++) { if ((recv) -eq $null) { break } } }
drain

# ─── 1. HELLO ───
Write-Host "`n[1] HELLO"
send 'HELLO'; $line = recv
if ($line -notmatch 'OK HELLO') { $line = recv }
if ($line -match 'var_count=(\d+)') {
    if ([int]$Matches[1] -eq $allVars.Count) { ok "var_count=$($allVars.Count)" }
    else { nok "var_count: expected $($allVars.Count) got $($Matches[1])" }
} else { nok "HELLO response" $line }

# ─── 2. LIST ───
Write-Host "`n[2] LIST"
send 'LIST'
$ids = @{}
$deadline = (Get-Date).AddMilliseconds(2000)
while ((Get-Date) -lt $deadline) {
    $l = recv
    if ($l -eq $null) { Start-Sleep 0.05; continue }
    if ($l -eq '' -or $l -eq 'OK LIST') { break }
    if ($l -match '^var id=(\d+)') { $ids[[int]$Matches[1]] = $true }
}
$missing = @()
foreach ($v in $allVars) { if (-not $ids[$v.id]) { $missing += "$($v.name)($($v.id))" } }
if ($missing.Count -eq 0) { ok "all $($allVars.Count) variables listed" }
else { nok "LIST missing: $($missing -join ' ')" }

# ─── 3. GVL watch/read ───
Write-Host "`n[3] GVL scalars ($($gvlS.Count) vars)"
if ($gvlS.Count -gt 0) {
    $idStr = ($gvlS | ForEach-Object { $_.id }) -join ' '
    send "WATCH $idStr"; drain
    $waitMs = [Math]::Max(300, $CycleUs * 3 / 1000)
    Start-Sleep -Milliseconds $waitMs
    send 'READ'; recv | Out-Null
    $samples = @{}
    $deadline = (Get-Date).AddMilliseconds(1500)
    while ((Get-Date) -lt $deadline) {
        $l = recv
        if ($l -eq $null) { Start-Sleep 0.02; continue }
        if ($l -eq '' -or $l -eq 'OK LIST') { break }
        if ($l -match 'sample id=(\d+) size=(\d+) hex=(\S+) forced=(\d)') {
            $samples[[int]$Matches[1]] = $Matches[3]
        }
    }
    foreach ($v in $gvlS) {
        $h = $samples[$v.id]
        if ($h) { ok "$($v.name) = $h ($($v.type))" }
        else    { nok "$($v.name)" "not in snapshot" }
    }
} else { ok "(no GVL scalars)" }

# ─── 4. INPUT force ───
Write-Host "`n[4] INPUT force ($($inVars.Count) vars)"
$done = 0
foreach ($v in $inVars) {
    if ($v.access -ne 'FORCEABLE') { continue }
    $hex = ($v.type -eq 'BOOL') ? '01' : '0100'
    send "FORCE $($v.id) $hex"; drain
    send "WATCH $($v.id)"; drain
    Start-Sleep -Milliseconds ([Math]::Max(300, $CycleUs * 3 / 1000))
    send 'READ'; recv | Out-Null
    $l = recv
    if ($l -match "id=$($v.id).*forced=1") { ok "$($v.name) forced"; $done++ }
    else { nok "$($v.name)" $l }
    send "UNFORCE $($v.id)"; drain
}
if ($done -eq 0) { ok "(no forceable INPUT vars)" }

# ─── 5. OUTPUT force ───
Write-Host "`n[5] OUTPUT force ($($outVars.Count) vars)"
$done = 0
foreach ($v in $outVars) {
    if ($v.access -ne 'FORCEABLE') { continue }
    $hex = ($v.type -eq 'BOOL') ? '01' : '0100'
    send "FORCE $($v.id) $hex"; drain
    send "WATCH $($v.id)"; drain
    Start-Sleep -Milliseconds ([Math]::Max(300, $CycleUs * 3 / 1000))
    send 'READ'; recv | Out-Null
    $l = recv
    if ($l -match "id=$($v.id).*forced=1") { ok "$($v.name) forced"; $done++ }
    else { nok "$($v.name)" $l }
    send "UNFORCE $($v.id)"; drain
}
if ($done -eq 0) { ok "(no forceable OUTPUT vars)" }

# ─── 6. Array RANGE ───
Write-Host "`n[6] Array RANGE ($($arrs.Count) arrays)"
foreach ($v in $arrs) {
    $sz = $v.size * $v.count
    send "RANGE GVL $($v.offset) $sz"
    $r = recv
    if ($r -match 'OK RANGE') { ok "$($v.name) offset=$($v.offset) size=$sz" }
    else { nok "$($v.name)" $r }
}
if ($arrs.Count -eq 0) { ok "(no arrays)" }

# ─── 7. Invalid ID ───
Write-Host "`n[7] Invalid ID"
send 'FORCE 99999 01'; $r = recv
if ($r -match 'OK|ERR') { ok "handled" } else { nok "no response" $r }

# ─── 8. DIAG ───
Write-Host "`n[8] DIAG"
send 'DIAG'; $r = recv
if ($r -match 'OK DIAG') { ok $r } else { nok "DIAG" $r }

# ─── 9. Release build ───
Write-Host "`n[9] Release build"
send 'QUIT'; Start-Sleep 0.5; $s.Close(); $c.Close(); Start-Sleep 0.5; $svr.Kill(); Start-Sleep 1
$relBin = $Binary -replace '_dbg\.exe$', '_stub.exe'
if ($relBin -eq $Binary) { $relBin = $Binary -replace '\.exe$', '_stub.exe' }
if (Test-Path $relBin) {
    $rp = Start-Process -FilePath $relBin -ArgumentList "--cycle-us 10000 --diag-interval 99" -NoNewWindow -PassThru
    Start-Sleep 2
    $has = $false
    try { $tc = [System.Net.Sockets.TcpClient]::new($HostAddr, $Port); $has = $true; $tc.Close() }
    catch { $has = $false }
    if (-not $has) { ok "no debug port on release" } else { nok "Release" "debug port open" }
    $rp.Kill()
} else { ok "(no release binary)" }

Write-Host "`n=== $P passed, $F failed ===" -F $(if($F){'Red'}else{'Green'})
exit $F
