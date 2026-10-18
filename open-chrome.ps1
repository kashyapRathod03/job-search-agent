Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "Launching Google Chrome for LinkedIn Agent (Port: 9222)" -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

$profile = "$env:USERPROFILE\.chrome-agent-profile"
$chromePaths = @(
    "C:\Program Files\Google\Chrome\Application\chrome.exe",
    "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
)

$found = $null
foreach ($path in $chromePaths) {
    if (Test-Path $path) {
        $found = $path
        break
    }
}

if ($found) {
    Start-Process -FilePath $found -ArgumentList "--remote-debugging-port=9222", "--user-data-dir=`"$profile`"", "https://www.linkedin.com/login"
} else {
    Start-Process -FilePath "chrome.exe" -ArgumentList "--remote-debugging-port=9222", "--user-data-dir=`"$profile`"", "https://www.linkedin.com/login"
}

Write-Host "`nChrome has opened!" -ForegroundColor Green
Write-Host "1. In that Chrome window, log in to LinkedIn (solve any 2FA/CAPTCHA)." -ForegroundColor Yellow
Write-Host "2. Once you reach your LinkedIn feed, run in terminal:" -ForegroundColor Yellow
Write-Host "   .\gradlew.bat bootRun" -ForegroundColor White
Write-Host "=======================================================================" -ForegroundColor Cyan
