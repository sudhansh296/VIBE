# Launch VIBE and walk splash -> login -> OTP -> profile setup -> home with demo values.
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$T = "C:\VIBE\tools\tap.ps1"
& $adb shell am force-stop com.vibe.app
& $adb shell am start -n com.vibe.app/.MainActivity | Out-Null
Start-Sleep 8
& $T "Enter 10-digit" | Out-Null; & $adb shell input text 9876543210; & $adb shell input keyevent KEYCODE_BACK; Start-Sleep 1
& $T "Send OTP" | Out-Null; Start-Sleep 3
& $adb shell input text 482913; Start-Sleep 3
& $T "What should people" | Out-Null; & $adb shell input text Sudhanshu; & $adb shell input keyevent KEYCODE_BACK; Start-Sleep 1
& $T "Male" | Out-Null
foreach ($p in @(@('Day', '6'), @('Month', 'June'), @('Year', '2008'))) { & $T $p[0] | Out-Null; Start-Sleep 1; & $T $p[1] | Out-Null; Start-Sleep 1 }
& $T "Continue" | Out-Null; Start-Sleep 3
"at home"
