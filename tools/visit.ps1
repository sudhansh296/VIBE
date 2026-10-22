param([string]$App, [string]$Name, [int]$X, [int]$Y, [switch]$NoBack)
# Tap (X,Y), capture the resulting page, then go back. Dismisses Vato fake calls around each step.
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$d = "C:\VIBE\tools\vato-dismiss.ps1"
"=== $Name"
if ($App -eq 'vato') { & $d }
& $adb shell input tap $X $Y; Start-Sleep 3
if ($App -eq 'vato') { & $d }
& C:\VIBE\tools\snap.ps1 $App $Name
if (-not $NoBack) { & $adb shell input keyevent KEYCODE_BACK; Start-Sleep 2 }
