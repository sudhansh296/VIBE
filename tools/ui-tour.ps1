# Walk the main screens on the phone and save one contact sheet per group (for a quick visual review).
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$T = "C:\VIBE\tools\tap.ps1"
$s = "C:\Users\sudha\AppData\Local\Temp\claude\C--VIBE\da61e9ff-8e5d-42c3-a39e-0a191438dcc4\scratchpad"
function Shot($n) { cmd /c "`"$adb`" exec-out screencap -p > `"$s\t_$n.png`""; $script:shots += $n }
function Back { & $adb shell input keyevent KEYCODE_BACK; Start-Sleep 1.5 }
$script:shots = @()

& $adb shell am force-stop com.vibe.app
& $adb shell am start -n com.vibe.app/.MainActivity | Out-Null
Start-Sleep 9
Shot home
& $adb shell input swipe 540 1800 540 900 300; Start-Sleep 1; Shot home2
& $adb shell input swipe 540 900 540 1800 300; Start-Sleep 1
& $T "Priya, 24" | Out-Null; Start-Sleep 2; Shot profile; Back
& $T "Filter" | Out-Null; Start-Sleep 2; Shot filter; Back
& $T "Voice call" | Out-Null; Start-Sleep 2; Shot voice_ring; Start-Sleep 3; Shot voice_live
& $T "VIDEO" | Out-Null; Start-Sleep 3.5; Shot video_live
& $T "End call" | Out-Null; Start-Sleep 1.5; Shot end_sheet
& $T "End call" 1 | Out-Null; Start-Sleep 2; Shot rate
& $T "SKIP" | Out-Null; Start-Sleep 2
& $T "Video call" | Out-Null; Start-Sleep 2; Shot outgoing; Start-Sleep 4; Back; Start-Sleep 1
& $adb shell "dumpsys window | grep -q vibe" ; & $T "🔔" | Out-Null; Start-Sleep 2; Shot incoming; Back
& $T "Random Match" | Out-Null; Start-Sleep 2; Shot match; Back

# contact sheets, 5 per row, 216px wide each
Add-Type -AssemblyName System.Drawing
$w = 216; $h = 480
for ($k = 0; $k -lt $script:shots.Count; $k += 5) {
  $set = $script:shots[$k..([Math]::Min($k + 4, $script:shots.Count - 1))]
  $sheet = New-Object System.Drawing.Bitmap ($w * $set.Count), $h
  $g = [System.Drawing.Graphics]::FromImage($sheet)
  for ($i = 0; $i -lt $set.Count; $i++) { $im = [System.Drawing.Image]::FromFile("$s\t_$($set[$i]).png"); $g.DrawImage($im, $i * $w, 0, $w, $h); $im.Dispose() }
  $sheet.Save("$s\tour_$($k / 5).jpg", [System.Drawing.Imaging.ImageFormat]::Jpeg); $g.Dispose(); $sheet.Dispose()
  "tour_$($k / 5).jpg: $($set -join ', ')"
}
