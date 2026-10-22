param([string]$Pts, [string]$Sheet = "walk", [int]$Back = 1)
# App stays open: for each "x,y" point tap it (phone pixels), screenshot, press BACK. Then build a contact sheet.
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$env:ANDROID_SERIAL = "10BD6L17N1000GE"
$s = "C:\Users\sudha\AppData\Local\Temp\claude\C--VIBE\da61e9ff-8e5d-42c3-a39e-0a191438dcc4\scratchpad"
$list = $Pts -split ";"
$i = 0
foreach ($p in $list) {
  $xy = $p -split ","
  & $adb shell input tap $xy[0] $xy[1] | Out-Null
  Start-Sleep -Milliseconds 1800
  cmd /c "`"$adb`" exec-out screencap -p > `"$s\w_$i.png`""
  for ($b = 0; $b -lt $Back; $b++) { & $adb shell input keyevent KEYCODE_BACK | Out-Null; Start-Sleep -Milliseconds 700 }
  $i++
}
$py = @"
from PIL import Image
n=$i
W,H=270,602
cols=min(n,5); rows=(n+cols-1)//cols
sh=Image.new('RGB',(cols*W,rows*H),(40,40,40))
for k in range(n):
    im=Image.open(r'$s\w_%d.png'%k).convert('RGB').resize((W,H))
    sh.paste(im,((k%cols)*W,(k//cols)*H))
sh.save(r'$s\$Sheet.jpg',quality=85)
"@
$py | python -
"$s\$Sheet.jpg"
