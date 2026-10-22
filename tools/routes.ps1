param([string[]]$Routes, [string]$Sheet = "sheet")
# Launch each debug route on the phone, screenshot it, then build one contact sheet (scratchpad\<Sheet>.jpg).
$adb = "C:\VIBE\tools\platform-tools\adb.exe"
$env:ANDROID_SERIAL = "10BD6L17N1000GE"
$s = "C:\Users\sudha\AppData\Local\Temp\claude\C--VIBE\da61e9ff-8e5d-42c3-a39e-0a191438dcc4\scratchpad"
& $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
$i = 0
foreach ($r in $Routes) {
  & $adb shell am force-stop com.vibe.callme | Out-Null
  & $adb shell am start -n com.vibe.callme/com.vibe.app.MainActivity --es route $r | Out-Null
  Start-Sleep -Milliseconds 5500
  cmd /c "`"$adb`" exec-out screencap -p > `"$s\rt_$i.png`""
  $i++
}
$py = @"
from PIL import Image, ImageDraw
import sys
n=$i
names='$($Routes -join "|")'.split('|')
W,H=300,660
cols=min(n,4); rows=(n+cols-1)//cols
sh=Image.new('RGB',(cols*W,rows*(H+18)),(40,40,40))
d=ImageDraw.Draw(sh)
for k in range(n):
    im=Image.open(r'$s\rt_%d.png'%k).convert('RGB').resize((W,H))
    x=(k%cols)*W; y=(k//cols)*(H+18)
    sh.paste(im,(x,y+18)); d.text((x+4,y+3),names[k],fill=(255,255,255))
sh.save(r'$s\$Sheet.jpg',quality=82)
"@
$py | python -
"$s\$Sheet.jpg"
