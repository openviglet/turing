@echo off
rem Run the plugin against a local WordPress and a local Turing (docker-compose.yml next to
rem this file).
rem
rem   run-docker.cmd            start both, install WordPress and provision Turing on first run
rem   run-docker.cmd multisite  also start a two-site multisite network on WP_MS_PORT (8081),
rem                             sharing the SN site SN_MS_SITE (Network), for the browser tests
rem   run-docker.cmd demo       also start the demo storefront on WP_DEMO_PORT (38082): a WordPress
rem                             of its own with the Tarnfell theme (demo/theme), WooCommerce and
rem                             its catalog, on its own SN site SN_DEMO_SITE (Demo), in
rem                             DEMO_LOCALE (en_US; pt_BR seeds the catalog in Portuguese)
rem   run-docker.cmd demo reset stop the demo and delete its WordPress and database only
rem   run-docker.cmd stop       stop the containers, keep the data
rem   run-docker.cmd reset      stop and delete WordPress, its database and Turing's store
rem   run-docker.cmd logs [svc] follow a log: wordpress (default), turing or db
rem   run-docker.cmd wp ...     run WP-CLI, e.g. run-docker.cmd wp dumont queue status
rem
rem On first run the SN site is created in the local Turing and the plugin gets an API key for
rem it. Override with environment variables before calling: WP_PORT (8080), TURING_PORT (2700),
rem SN_SITE (Sample), TURING_LOCALE (en_US), TURING_ADMIN_PASSWORD (dumont-dev), TURING_IMAGE.
rem OPENAI_API_KEY turns on Turing's generative AI (model: TURING_STARTUP_DEFAULT_LLM_MODEL,
rem gpt-4o-mini); a store that already holds an LLM instance keeps it.
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

if /i "%~1"=="stop"  ( %DC% --profile multisite --profile demo down & exit /b %ERRORLEVEL% )
if /i "%~1"=="reset" ( %DC% --profile multisite --profile demo down -v & exit /b %ERRORLEVEL% )
if /i "%~1"=="multisite" goto :multisite
if /i "%~1"=="demo" goto :demo
if /i "%~1"=="logs"  goto :logs
if /i "%~1"=="wp" goto :wp
if not "%~1"=="" ( echo Unknown command: %~1 & exit /b 1 )

rem The admin screens are built JS (npm run build); src/ is mounted as is, so build it here.
set "BUILT=1"
for %%e in (health mapping setup tuning insights turing-sdk) do if not exist src\admin\build\%%e.asset.php set "BUILT="
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

:demo
rem The demo has a WordPress and an SN site of its own (D219): the browser tests' site is
rem never touched, so both are up at once.
if "%WP_DEMO_PORT%"=="" set "WP_DEMO_PORT=38082"
if "%SN_DEMO_SITE%"=="" set "SN_DEMO_SITE=Demo"
if "%DEMO_LOCALE%"=="" set "DEMO_LOCALE=en_US"
if /i "%~2"=="reset" (
    %DC% --profile demo rm -s -f wordpress-demo cron-demo db-demo
    docker volume rm viglet-dumont-wp_wp-demo viglet-dumont-wp_db-demo
    exit /b 0
)
rem The single site and Turing first: the demo shares that Turing.
call "%~f0" || exit /b 1
set "WPD=%DC% run --rm --no-deps cli-demo wp"
set "DEMOURL=http://localhost:%WP_DEMO_PORT%"
%DC% --profile demo up -d || exit /b 1

echo Waiting for the demo WordPress...
set /a TRIES=0
:waitdemo
set /a TRIES+=1
%WPD% db check >nul 2>&1 && goto :readydemo
if %TRIES% geq 60 ( echo The demo WordPress did not come up. See: run-docker.cmd logs wordpress-demo & exit /b 1 )
timeout /t 2 /nobreak >nul
goto :waitdemo

:readydemo
%WPD% core is-installed >nul 2>&1
if errorlevel 1 (
    echo Installing the demo WordPress...
    %WPD% core install --url=%DEMOURL% --title="Tarnfell" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
)
%WPD% plugin activate viglet-dumont >nul 2>&1

set "KEY="
for /f "delims=" %%k in ('%WPD% option pluck viglet_dumont_settings api_key 2^>nul') do set "KEY=%%k"
if "%KEY%"=="" (
    for /f "delims=" %%k in ('powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_DEMO_SITE%" -Locale "%DEMO_LOCALE%"') do set "KEY=%%k"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_DEMO_SITE%" -Locale "%DEMO_LOCALE%" -NoToken
)
if "%KEY%"=="" ( echo Turing was not provisioned for %SN_DEMO_SITE%; the demo has no API key. & exit /b 1 )
%WPD% eval "$s = get_option( 'viglet_dumont_settings', array() ); $s['api_key'] = '%KEY%'; update_option( 'viglet_dumont_settings', $s );" || exit /b 1

%WPD% theme activate tarnfell || exit /b 1
rem The site speaks DEMO_LOCALE, and the seed writes the catalog in it (D217).
if /i not "%DEMO_LOCALE%"=="en_US" %WPD% language core install %DEMO_LOCALE% >nul 2>&1
%WPD% site switch-language %DEMO_LOCALE% || exit /b 1
%WPD% rewrite structure "/%%postname%%/" --hard || exit /b 1
rem A shop: WooCommerce on, with the demo's catalog (demo/seed), which runs demo/setup.php first.
%WPD% plugin is-installed woocommerce >nul 2>&1 || %WPD% plugin install woocommerce || exit /b 1
%WPD% plugin activate woocommerce || exit /b 1
if /i not "%DEMO_LOCALE%"=="en_US" %WPD% language plugin install woocommerce %DEMO_LOCALE% >nul 2>&1
%WPD% eval-file /demo/seed/catalog.php || exit /b 1
rem The seed makes products indexable after saving them, so nothing queued them: send them now.
%WPD% dumont reindex || exit /b 1

echo.
echo Demo storefront: %DEMOURL%/   (admin / admin), SN site %SN_DEMO_SITE%
echo Demo WP-CLI:     docker compose run --rm --no-deps cli-demo wp ...
exit /b 0

:multisite
rem The single site and Turing first: the network shares that Turing.
call "%~f0" || exit /b 1
if "%WP_MS_PORT%"=="" set "WP_MS_PORT=8081"
if "%SN_MS_SITE%"=="" set "SN_MS_SITE=Network"
set "WPMS=%DC% run --rm --no-deps cli-ms wp"
set "MSURL=http://localhost:%WP_MS_PORT%"
%DC% --profile multisite up -d || exit /b 1

echo Waiting for the multisite WordPress...
set /a TRIES=0
:waitms
set /a TRIES+=1
%WPMS% db check >nul 2>&1 && goto :readyms
if %TRIES% geq 60 ( echo The multisite WordPress did not come up. See: run-docker.cmd logs wordpress-ms & exit /b 1 )
timeout /t 2 /nobreak >nul
goto :waitms

:readyms
%WPMS% core is-installed --network >nul 2>&1
if errorlevel 1 (
    echo Installing the multisite network...
    %WPMS% core multisite-install --url=%MSURL% --title="Viglet Dumont network" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
    %DC% run --rm --no-deps --entrypoint sh cli-ms -c "cp /dumont/multisite.htaccess /var/www/html/.htaccess" || exit /b 1
    %WPMS% site create --slug=second --title="Second site" --email=admin@example.org || exit /b 1
)
%WPMS% plugin activate viglet-dumont --network >nul 2>&1

rem One key for the network's SN site, stored on both of its sites.
set "KEY="
for /f "delims=" %%k in ('%WPMS% option pluck viglet_dumont_settings api_key 2^>nul') do set "KEY=%%k"
if "%KEY%"=="" (
    for /f "delims=" %%k in ('powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_MS_SITE%" -Locale "%TURING_LOCALE%"') do set "KEY=%%k"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_MS_SITE%" -Locale "%TURING_LOCALE%" -NoToken
)
if "%KEY%"=="" ( echo Turing was not provisioned for %SN_MS_SITE%; the network has no API key. & exit /b 1 )
for %%u in (%MSURL%/ %MSURL%/second/) do %WPMS% --url=%%u eval "$s = get_option( 'viglet_dumont_settings', array() ); $s['api_key'] = '%KEY%'; update_option( 'viglet_dumont_settings', $s );" || exit /b 1

echo.
echo Network:    %MSURL%/wp-admin/network/   (admin / admin), sites / and /second/, SN site %SN_MS_SITE%
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
