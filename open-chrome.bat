@echo off
echo =======================================================================
echo Launching Google Chrome for LinkedIn Agent (Port: 9222)
echo =======================================================================

set "PROFILE=%USERPROFILE%\.chrome-agent-profile"

if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --user-data-dir="%PROFILE%" https://www.linkedin.com/login
) else if exist "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" (
    start "" "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --user-data-dir="%PROFILE%" https://www.linkedin.com/login
) else (
    start "" chrome.exe --remote-debugging-port=9222 --user-data-dir="%PROFILE%" https://www.linkedin.com/login
)

echo.
echo Chrome has opened!
echo 1. In that Chrome window, log in to LinkedIn (solve any 2FA/CAPTCHA).
echo 2. Once you reach your LinkedIn feed, run in terminal:
echo    .\gradlew.bat bootRun
echo =======================================================================
