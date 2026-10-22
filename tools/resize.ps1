param([string]$Name)
$s = "C:\Users\sudha\AppData\Local\Temp\claude\C--VIBE\da61e9ff-8e5d-42c3-a39e-0a191438dcc4\scratchpad"
Add-Type -AssemblyName System.Drawing
$b = [System.Drawing.Image]::FromFile("$s\$Name.png")
$t = New-Object System.Drawing.Bitmap 270, ([int](270 * $b.Height / $b.Width))
$g = [System.Drawing.Graphics]::FromImage($t); $g.DrawImage($b, 0, 0, $t.Width, $t.Height)
$t.Save("$s\$Name.jpg", [System.Drawing.Imaging.ImageFormat]::Jpeg); $b.Dispose()
