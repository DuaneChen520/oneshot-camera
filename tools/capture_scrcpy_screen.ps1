param(
    [string]$Device = "192.168.6.104:45957",
    [string]$OutputPath = "preview\scrcpy_screen.png"
)

$adb = "E:\AndoirdSDK\Sdk\platform-tools\adb.exe"

# 1. Ensure connection
$null = & $adb connect $Device

# 2. Query SurfaceFlinger for scrcpy virtual display
$flingerOut = & $adb -s $Device shell dumpsys SurfaceFlinger --display-id 2>&1
$virtualDisplayId = $null

foreach ($line in ($flingerOut -split "`r?`n")) {
    if ($line -match 'Display\s+(\d+)\s*\((scrcpy)\)') {
        $virtualDisplayId = $matches[1]
        break
    }
    if ($line -match '(\d+).*displayName="scrcpy"') {
        $virtualDisplayId = $matches[1]
        break
    }
}

if (-not $virtualDisplayId) {
    # Fallback to search any virtual display
    $displays = & $adb -s $Device shell dumpsys SurfaceFlinger --display-id 2>&1
    foreach ($line in ($displays -split "`r?`n")) {
        if ($line -match 'Display\s+(\d+)') {
            $idCandidate = $matches[1]
            if ($idCandidate -ne "0" -and $idCandidate -ne "1152921504606846976") {
                $virtualDisplayId = $idCandidate
                break
            }
        }
    }
}

if ($virtualDisplayId) {
    Write-Host "[OneShot 截图] 成功锁定 scrcpy 专属副屏 ID: $virtualDisplayId"
    & $adb -s $Device shell screencap -d $virtualDisplayId /data/local/tmp/oneshot_scrcpy_frame.png
} else {
    Write-Warning "[OneShot 截图] 未检测到 scrcpy 虚拟副屏，正在回退截取默认画面..."
    & $adb -s $Device shell screencap -p /data/local/tmp/oneshot_scrcpy_frame.png
}

# Pull file
$outDir = Split-Path -Parent $OutputPath
if ($outDir -and -not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}
& $adb -s $Device pull /data/local/tmp/oneshot_scrcpy_frame.png $OutputPath
Write-Host "[OneShot 截图] 画面已存入 -> $OutputPath"
