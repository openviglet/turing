@echo off
rem Run the plugin against a local WordPress and a local Turing (docker-compose.yml next to
rem this file).
rem
rem   run-docker.cmd            start both, install WordPress and provision Turing on first run
rem   run-docker.cmd stop       stop the containers, keep the data
rem   run-docker.cmd reset      stop and delete WordPress, its database and Turing's store
rem   run-docker.cmd logs [svc] follow a log: wordpress (default), turing or db
rem   run-docker.cmd wp ...     run WP-CLI, e.g. run-docker.cmd wp dumont queue status
rem
rem On first run the SN site is created in the local Turing and the plugin gets an API key for
rem it. Override with environment variables before calling: WP_PORT (8080), TURING_PORT (2700),
rem SN_SITE (Sample), TURING_LOCALE (en_US), TURING_ADMIN_PASSWORD (dumont-dev), TURING_IMAGE.
rem Logins: WordPress admin / admin, Turing admin / %TURING_ADMIN_PASSWORD%.

setlocal
cd /d "%~dp0"

if "%WP_PORT%"=="" set "WP_PORT=8080"
if "%TURING_PORT%"=="" set "TURING_PORT=2700"
if "%SN_SITE%"=="" set "SN_SITE=Sample"
if "%TURING_LOCALE%"=="" set "TURING_LOCALE=en_US"
if "%TURING_ADMIN_PASSWORD%"=="" set "TURING_ADMIN_PASSWORD=dumont-dev"
rem A Turing built from a local checkout wins over the published image, as in trabalharei.
if "%TURING_IMAGE%"=="" docker image inspect turing-ce:local >nul 2>&1 && set "TURING_IMAGE=turing-ce:local"

set "DC=docker compose"
set "WP=%DC% run --rm --no-deps cli wp"

if /i "%~1"=="stop"  ( %DC% down & exit /b %ERRORLEVEL% )
if /i "%~1"=="reset" ( %DC% down -v & exit /b %ERRORLEVEL% )
if /i "%~1"=="logs"  goto :logs
if /i "%~1"=="wp" goto :wp
if not "%~1"=="" ( echo Unknown command: %~1 & exit /b 1 )

rem The admin screens are built JS (npm run build); src/ is mounted as is, so build it here.
set "BUILT=1"
for %%e in (health mapping setup) do if not exist src\admin\build\%%e.asset.php set "BUILT="
if not defined BUILT (
    echo Building the admin screens...
    call npm ci --no-audit --no-fund || exit /b 1
    call npm run build || exit /b 1
)

%DC% up -d || exit /b 1

echo Waiting for WordPress...
set /a TRIES=0
:wait
set /a TRIES+=1
%WP% db check >nul 2>&1 && goto :ready
if %TRIES% geq 60 ( echo WordPress did not come up. See: run-docker.cmd logs & exit /b 1 )
timeout /t 2 /nobreak >nul
goto :wait

:ready
%WP% core is-installed >nul 2>&1
if errorlevel 1 (
    echo Installing WordPress...
    %WP% core install --url=http://localhost:%WP_PORT% --title="Viglet Dumont test" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
    %WP% plugin activate viglet-dumont || exit /b 1
    %WP% option patch update viglet_dumont_settings server_url http://turing:2700
    %WP% option patch update viglet_dumont_settings site_name "%SN_SITE%"
    %WP% option patch update viglet_dumont_settings locale "%TURING_LOCALE%"
) else (
    %WP% plugin activate viglet-dumont >nul 2>&1
)

rem The SN site is converged on every start; a key is minted only while the plugin has none.
set "KEY="
for /f "delims=" %%k in ('%WP% option pluck viglet_dumont_settings api_key 2^>nul') do set "KEY=%%k"
if "%KEY%"=="" (
    set "NEWKEY="
    for /f "delims=" %%k in ('powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_SITE%" -Locale "%TURING_LOCALE%"') do set "NEWKEY=%%k"
    call :setkey
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_SITE%" -Locale "%TURING_LOCALE%" -NoToken
)

echo.
echo WordPress:  http://localhost:%WP_PORT%/wp-admin   (admin / admin)
echo Plugin:     http://localhost:%WP_PORT%/wp-admin/admin.php?page=viglet-dumont
echo Turing:     http://localhost:%TURING_PORT%   (admin / %TURING_ADMIN_PASSWORD%), SN site %SN_SITE%
echo WP-CLI:     run-docker.cmd wp dumont queue status
exit /b 0

:setkey
if "%NEWKEY%"=="" ( echo Turing was not provisioned; the plugin has no API key yet. & exit /b 0 )
%WP% option patch update viglet_dumont_settings api_key "%NEWKEY%" >nul
echo The plugin has an API key for the local Turing.
exit /b 0

:logs
set "SVC=%~2"
if "%SVC%"=="" set "SVC=wordpress"
%DC% logs -f %SVC%
exit /b %ERRORLEVEL%

:wp
rem %* verbatim minus the leading "wp ": shift/%1 would split --name=value at the "=".
set "ARGS=%*"
set "ARGS=%ARGS:~3%"
%WP% %ARGS%
exit /b %ERRORLEVEL%
