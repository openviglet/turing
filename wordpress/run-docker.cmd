@echo off
rem Run the plugin in a local WordPress (docker-compose.yml next to this file).
rem
rem   run-docker.cmd            start, install WordPress on first run, activate the plugin
rem   run-docker.cmd stop       stop the containers, keep the data
rem   run-docker.cmd reset      stop and delete WordPress and its database
rem   run-docker.cmd logs       follow the WordPress log
rem   run-docker.cmd wp ...     run WP-CLI, e.g. run-docker.cmd wp dumont queue status
rem
rem On first run the plugin is pointed at Turing on the host. Override with environment
rem variables before calling: WP_PORT (8080), TURING_URL (http://host.docker.internal:2700),
rem TURING_API_KEY, SN_SITE (Sample), TURING_LOCALE (en_US). Admin login: admin / admin.

setlocal
cd /d "%~dp0"

if "%WP_PORT%"=="" set "WP_PORT=8080"
if "%TURING_URL%"=="" set "TURING_URL=http://host.docker.internal:2700"
if "%SN_SITE%"=="" set "SN_SITE=Sample"
if "%TURING_LOCALE%"=="" set "TURING_LOCALE=en_US"

set "DC=docker compose"

if /i "%~1"=="stop"  ( %DC% down & exit /b %ERRORLEVEL% )
if /i "%~1"=="reset" ( %DC% down -v & exit /b %ERRORLEVEL% )
if /i "%~1"=="logs"  ( %DC% logs -f wordpress & exit /b %ERRORLEVEL% )
if /i "%~1"=="wp" goto :wp
if not "%~1"=="" ( echo Unknown command: %~1 & exit /b 1 )

%DC% up -d || exit /b 1

echo Waiting for WordPress...
set /a TRIES=0
:wait
set /a TRIES+=1
%DC% run --rm --no-deps cli wp db check >nul 2>&1 && goto :ready
if %TRIES% geq 60 ( echo WordPress did not come up. See: run-docker.cmd logs & exit /b 1 )
timeout /t 2 /nobreak >nul
goto :wait

:ready
%DC% run --rm --no-deps cli wp core is-installed >nul 2>&1
if errorlevel 1 (
    echo Installing WordPress...
    %DC% run --rm --no-deps cli wp core install --url=http://localhost:%WP_PORT% --title="Viglet Dumont test" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
    %DC% run --rm --no-deps cli wp plugin activate viglet-dumont || exit /b 1
    %DC% run --rm --no-deps cli wp option patch update viglet_dumont_settings server_url "%TURING_URL%"
    %DC% run --rm --no-deps cli wp option patch update viglet_dumont_settings site_name "%SN_SITE%"
    %DC% run --rm --no-deps cli wp option patch update viglet_dumont_settings locale "%TURING_LOCALE%"
    if not "%TURING_API_KEY%"=="" %DC% run --rm --no-deps cli wp option patch update viglet_dumont_settings api_key "%TURING_API_KEY%"
) else (
    %DC% run --rm --no-deps cli wp plugin activate viglet-dumont >nul 2>&1
)

echo.
echo WordPress:  http://localhost:%WP_PORT%/wp-admin   (admin / admin)
echo Plugin:     http://localhost:%WP_PORT%/wp-admin/admin.php?page=viglet-dumont
echo WP-CLI:     run-docker.cmd wp dumont queue status
exit /b 0

:wp
rem %* verbatim minus the leading "wp ": shift/%1 would split --name=value at the "=".
set "ARGS=%*"
set "ARGS=%ARGS:~3%"
%DC% run --rm --no-deps cli wp %ARGS%
exit /b %ERRORLEVEL%
