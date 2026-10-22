param([string]$Text, [int]$Index = 0)
# Tap the on-screen element whose text or content-desc contains $Text (uses a uiautomator dump).
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$xml = (& $adb shell "uiautomator dump --compressed /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml") -join ''
if ($xml -notmatch '<hierarchy') { "no ui dump"; exit 1 }
$nodes = ([xml]$xml).SelectNodes("//node") | Where-Object { $_.text -like "*$Text*" -or $_.'content-desc' -like "*$Text*" }
$n = @($nodes)[$Index]
if (-not $n) { "not found: $Text"; exit 1 }
$b = [regex]::Matches($n.bounds, '\d+') | ForEach-Object { [int]$_.Value }
$x = [int](($b[0] + $b[2]) / 2); $y = [int](($b[1] + $b[3]) / 2)
& $adb shell input tap $x $y
"tapped '$Text' at $x,$y"
