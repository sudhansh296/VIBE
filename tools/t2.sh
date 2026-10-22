# Phone helper for the two-phone test. usage: t2.sh SERIAL tap X Y | key CODE | text STR | swipe X1 Y1 X2 Y2 [ms] | shot NAME
# Safety: acts only when VIBE is the focused app (otherwise presses Home), never taps the top 350 px (notification banners).
A=/c/VIBE/tools/platform-tools/adb.exe; S=$1; shift
SP=/c/Users/sudha/AppData/Local/Temp/claude/C--VIBE/da61e9ff-8e5d-42c3-a39e-0a191438dcc4/scratchpad
if [ "$1" = shot ]; then
  $A -s $S exec-out screencap -p > "$SP/$2.png"
  powershell -NoProfile -File 'C:\VIBE\tools\resize.ps1' "$2"
  exit 0
fi
F=$($A -s $S shell "dumpsys window | grep mCurrentFocus")
echo "$F" | grep -q com.vibe.callme || { echo "NOT VIBE: $F -> Home"; $A -s $S shell input keyevent 3; exit 1; }
case $1 in
  tap) [ "$3" -lt 350 ] && { echo REFUSED top; exit 1; }; $A -s $S shell input tap $2 $3; echo "tap $2 $3";;
  key) $A -s $S shell input keyevent $2;;
  text) $A -s $S shell input text "$2";;
  swipe) $A -s $S shell input swipe $2 $3 $4 $5 ${6:-500};;
esac
