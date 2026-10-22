# Dismiss Vato's fake incoming-call screen (X, then Quit) if it is showing. Never taps Answer.
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
for ($i = 0; $i -lt 3; $i++) {
  $raw = (& $adb shell "rm -f /sdcard/ui.xml; uiautomator dump --compressed /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml 2>/dev/null") -join ""
  if ($raw -match 'Invite you on the call') {
    & $adb shell input tap 997 181; Start-Sleep 2
    & $adb shell input tap 540 2180; Start-Sleep 2   # "Quit" in hang-up confirm
    "dismissed fake call"
  } elseif ($raw -match 'Are you sure to hang up') {
    & $adb shell input tap 540 2180; Start-Sleep 2
  } else { return }
}
