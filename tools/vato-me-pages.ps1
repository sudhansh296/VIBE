# Relaunch Vato, clear launch popups, and capture the remaining "Me" sub-pages.
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$d = "C:\VIBE\tools\vato-dismiss.ps1"
$s = "C:\VIBE\tools\snap.ps1"
function Get-UiText { (& $adb shell "uiautomator dump --compressed /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml 2>/dev/null") -join '' }
function Close-Popups {
  for ($k = 0; $k -lt 6; $k++) {
    & $d
    $t = Get-UiText
    if ($t -match 'Start Check-in') { & $adb shell input tap 1028 1115; Start-Sleep 2 }
    elseif ($t -match 'Newcomer|SPECIAL') { & $adb shell input tap 538 1916; Start-Sleep 2 }
    elseif ($t -match 'Gems Balance|Discover|Chats') { return $true }
    else { Start-Sleep 2 }
  }
  return $false
}
& $adb shell monkey -p com.vatochat.video -c android.intent.category.LAUNCHER 1 2>&1 | Out-Null
Start-Sleep 5
Close-Popups | Out-Null
$pages = @(@('me_service', 300, 1708), @('me_level', 300, 1880), @('me_account', 300, 2050), @('me_settings', 1003, 154))
foreach ($p in $pages) {
  if (-not (Close-Popups)) { "lost state before $($p[0])"; continue }
  if ((Get-UiText) -notmatch 'Gems Balance') { & $adb shell input tap 900 2325; Start-Sleep 3 }
  if ((Get-UiText) -notmatch 'Gems Balance') { "not on Me tab, skipping $($p[0])"; continue }
  "=== $($p[0])"
  & $adb shell input tap $p[1] $p[2]; Start-Sleep 4
  & $d
  & $s vato $p[0] | Where-Object { $_ -notmatch '^\[true\]  \|  \|' } | Select-Object -First 30
  & $adb shell input keyevent KEYCODE_BACK; Start-Sleep 2
}
