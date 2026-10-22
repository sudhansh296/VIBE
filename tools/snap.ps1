param([string]$App, [string]$Name)
# Capture screenshot + visible UI text from the phone.
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$o = "C:\VIBE\research\$App"
New-Item -ItemType Directory -Force $o | Out-Null
cmd /c "`"$adb`" exec-out screencap -p > `"$o\$Name.png`""
$xml = $null
for ($i = 0; $i -lt 3 -and -not $xml; $i++) {
  $raw = (& $adb shell "rm -f /sdcard/ui.xml; uiautomator dump --compressed /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml 2>/dev/null") -join ""
  if ($raw -match '<hierarchy') { $xml = $raw }
}
if (-not $xml) { "UI dump failed"; return }
$xml | Out-File -Encoding utf8 "$o\$Name.xml"
([xml]$xml).SelectNodes("//node") |
  Where-Object { $_.text -or $_.'content-desc' -or $_.clickable -eq 'true' } |
  ForEach-Object { "[$($_.clickable)] $($_.text) | $($_.'content-desc') | $(($_.'resource-id') -replace '^.*:id/','') | $($_.bounds)" }
