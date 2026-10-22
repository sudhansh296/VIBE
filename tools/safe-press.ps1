param([int]$X, [int]$Y, [int]$HoldMs = 2500, [string]$Name = "press", [int]$CropX = 0, [int]$CropY = 0, [int]$CropW = 1080, [int]$CropH = 2400)
# Long-presses a point in VIBE and saves a screenshot taken WHILE it is pressed (to see the press feedback).
# Safety: only acts when VIBE is the focused app, and never in the top 350 px (notification banners appear there).
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$s = "C:\Users\sudha\AppData\Local\Temp\claude\C--VIBE\da61e9ff-8e5d-42c3-a39e-0a191438dcc4\scratchpad"
if ($Y -lt 350) { "REFUSED: too close to the top (notifications)"; exit 1 }
$focus = (& $adb shell "dumpsys window | grep mCurrentFocus") -join " "
if ($focus -notmatch "com.vibe.callme") { "NOT VIBE ($focus) - going Home"; & $adb shell input keyevent 3; exit 1 }
Start-Process -FilePath $adb -ArgumentList "shell", "input", "swipe", "$X", "$Y", "$X", "$Y", "$HoldMs" -WindowStyle Hidden
Start-Sleep -Milliseconds ([Math]::Min(1300, $HoldMs - 400))
cmd /c "`"$adb`" exec-out screencap -p > `"$s\$Name.png`""
Add-Type -AssemblyName System.Drawing
$b = [System.Drawing.Image]::FromFile("$s\$Name.png")
$w = [Math]::Min($CropW, $b.Width - $CropX); $h = [Math]::Min($CropH, $b.Height - $CropY)
$scale = [Math]::Min(1.0, 540.0 / $w)
$c = New-Object System.Drawing.Bitmap ([int]($w * $scale)), ([int]($h * $scale))
$g = [System.Drawing.Graphics]::FromImage($c)
$g.DrawImage($b, (New-Object System.Drawing.Rectangle 0, 0, $c.Width, $c.Height), (New-Object System.Drawing.Rectangle $CropX, $CropY, $w, $h), [System.Drawing.GraphicsUnit]::Pixel)
$c.Save("$s\$Name.jpg", [System.Drawing.Imaging.ImageFormat]::Jpeg); $b.Dispose()
Start-Sleep -Milliseconds ($HoldMs - 1000)
$focus = (& $adb shell "dumpsys window | grep mCurrentFocus") -join " "
if ($focus -notmatch "com.vibe.callme") { "LEFT VIBE after press ($focus)" } else { "ok $Name" }
