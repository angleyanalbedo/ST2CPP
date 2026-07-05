param(
    [switch]$Force,
    [string]$ToolsDir = (Join-Path (Split-Path $PSScriptRoot -Parent) ".env")
)

$ErrorActionPreference = "Stop"

# ─── 配置 ───
$ArmGccUrl  = "https://armkeil.blob.core.windows.net/developer/files/downloads/gnu/15.2.rel1/binrel/arm-gnu-toolchain-15.2.rel1-mingw-w64-i686-arm-none-eabi.zip"
$ArmGccOut  = Join-Path $ToolsDir "arm-gcc"
$OpenocdOut = Join-Path $ToolsDir "opencoded"

# xpack OpenOCD: 自动取最新版
$OpenocdRepo = "xpack-dev-tools/openocd-xpack"

# ─── 辅助函数 ───

function Write-Step($msg) {
    Write-Host "==> $msg" -ForegroundColor Cyan
}

function Test-Command($cmd) {
    return (Get-Command $cmd -ErrorAction SilentlyContinue) -ne $null
}

# ─── 前置检查 ───

if (-not (Test-Command "7z")) {
    Write-Host "[ERROR] 需要 7-Zip。安装: scoop install 7zip" -ForegroundColor Red
    exit 1
}

if (-not (Test-Command "Invoke-WebRequest")) {
    Write-Host "[ERROR] 需要 PowerShell 5+ 或 pwsh" -ForegroundColor Red
    exit 1
}

# ─── 1. ARM GCC ───

$ArmGccStamp = Join-Path $ArmGccOut ".download_done"
if ((Test-Path $ArmGccStamp) -and (-not $Force)) {
    Write-Step "ARM GCC 已下载 ($ArmGccOut)，跳过（用 -Force 重新下载）"
} else {
    Write-Step "下载 ARM GCC 工具链..."
    $zip = Join-Path $ToolsDir "arm-gcc-toolchain.zip"
    Invoke-WebRequest -Uri $ArmGccUrl -OutFile $zip -UserAgent "powershell"

    Write-Step "解压到 $ArmGccOut ..."
    if (Test-Path $ArmGccOut) { Remove-Item -Recurse -Force $ArmGccOut }
    New-Item -ItemType Directory -Force -Path $ArmGccOut | Out-Null
    7z x $zip -o"$ArmGccOut" -y | Out-Null
    Remove-Item $zip -Force

    # xpack ARM GCC 可能带版本号目录，剥离顶层
    $sub = Get-ChildItem -Path $ArmGccOut -Directory | Where-Object { $_.Name -like "arm-*" -or $_.Name -like "xpack-*" }
    if ($sub) {
        $tmp = Join-Path $ToolsDir "_arm_gcc_tmp"
        Move-Item -Path $ArmGccOut\* -Destination $tmp -ErrorAction SilentlyContinue
        Move-Item -Path "$tmp\$($sub[0].Name)\*" -Destination $ArmGccOut
        Remove-Item -Recurse -Force $tmp
    }

    # 验证
    $gcc = Join-Path $ArmGccOut "bin\arm-none-eabi-gcc.exe"
    if (-not (Test-Path $gcc)) {
        Write-Host "[ERROR] ARM GCC 未找到: $gcc" -ForegroundColor Red
        exit 1
    }
    & $gcc --version | Select-Object -First 1

    Set-Content -Path $ArmGccStamp -Value (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    Write-Step "ARM GCC 就绪"
}

# ─── 2. OpenOCD ───

$OpenocdStamp = Join-Path $OpenocdOut ".download_done"
if ((Test-Path $OpenocdStamp) -and (-not $Force)) {
    Write-Step "OpenOCD 已下载 ($OpenocdOut)，跳过（用 -Force 重新下载）"
} else {
    Write-Step "获取最新 OpenOCD 版本信息..."
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$OpenocdRepo/releases/latest" -Headers @{"User-Agent"="powershell"}
    $tag = $release.tag_name
    Write-Step "  最新版本: $tag"

    $asset = $release.assets | Where-Object {
        $_.name -like "*.zip" -and $_.name -like "*win32-x64*"
    } | Select-Object -First 1

    if (-not $asset) {
        Write-Host "[ERROR] 未找到 Windows x64 版本 OpenOCD" -ForegroundColor Red
        exit 1
    }

    Write-Step "下载 OpenOCD ($($asset.name))..."
    $zip = Join-Path $ToolsDir "openocd.zip"
    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip -UserAgent "powershell"

    Write-Step "解压到 $OpenocdOut ..."
    if (Test-Path $OpenocdOut) { Remove-Item -Recurse -Force $OpenocdOut }
    New-Item -ItemType Directory -Force -Path $OpenocdOut | Out-Null
    7z x $zip -o"$OpenocdOut" -y | Out-Null
    Remove-Item $zip -Force

    # xpack 压缩包含版本号顶层目录，剥离
    $sub = Get-ChildItem -Path $OpenocdOut -Directory
    if ($sub.Count -eq 1) {
        $tmp = Join-Path $ToolsDir "_openocd_tmp"
        Move-Item -Path "$OpenocdOut\$($sub[0].Name)\*" -Destination $tmp
        Remove-Item -Recurse -Force $OpenocdOut\*
        Move-Item -Path "$tmp\*" -Destination $OpenocdOut
        Remove-Item -Recurse -Force $tmp
    }

    $ocd = Join-Path $OpenocdOut "bin\openocd.exe"
    if (-not (Test-Path $ocd)) {
        Write-Host "[ERROR] OpenOCD 未找到: $ocd" -ForegroundColor Red
        exit 1
    }
    & $ocd --version | Select-Object -First 1

    Set-Content -Path $OpenocdStamp -Value (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    Write-Step "OpenOCD 就绪"
}

Write-Host ""
Write-Host "所有工具下载完成！" -ForegroundColor Green
Write-Host "  ARM GCC: $ArmGccOut" -ForegroundColor Gray
Write-Host "  OpenOCD: $OpenocdOut" -ForegroundColor Gray
