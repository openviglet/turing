@echo off
rem Run the plugin against a local WordPress and a local Turing (docker-compose.yml next to
rem this file).
rem
rem   run-docker.cmd            start both, install WordPress and provision Turing on first run
rem   run-docker.cmd multisite  also start a two-site multisite network on WP_MS_PORT (8081),
rem                             sharing the SN site SN_MS_SITE (Network), for the browser tests
rem   run-docker.cmd demo       also start the two demo storefronts (D283), each a WordPress of its
rem                             own with the Tarnfell theme (demo/theme), WooCommerce, its catalog
rem                             and its own SN site: Tarnfell in English on WP_DEMO_PORT (38082),
rem                             SN site SN_DEMO_SITE (tarnfell), and Recanto da Serra in Portuguese
rem                             on WP_DEMO_PT_PORT (38083), SN site SN_DEMO_PT_SITE
rem                             (recanto-da-serra)
rem   run-docker.cmd demo en|pt start one of them
rem   run-docker.cmd demo edu   also start the school (D325): Lumen School in English on
rem                             WP_DEMO_EDU_PORT (38085), SN site SN_DEMO_EDU_SITE (lumen-school),
rem                             the Lumen theme with an agenda, openings and a library, no
rem                             WooCommerce; `demo edu-pt` is Escola Lumen in Portuguese on
rem                             WP_DEMO_EDU_PT_PORT (38086), SN site SN_DEMO_EDU_PT_SITE (escola-lumen)
rem   run-docker.cmd demo reset en|pt|edu|edu-pt  stop one demo site and delete its WordPress and
rem                             database only (each is described by demo/sites/<key>.json)
rem   run-docker.cmd fresh      also start a WordPress the plugin was never installed on (D281), on
rem                             WP_FRESH_PORT (38084): Tarnfell's theme, WooCommerce and catalog,
rem                             for the developer video to install the release zip on
rem   run-docker.cmd fresh reset  delete it, to record that video again
rem   run-docker.cmd stop       stop the containers, keep the data
rem   run-docker.cmd reset      stop and delete WordPress, its database and Turing's store
rem   run-docker.cmd logs [svc] follow a log: wordpress (default), turing or db
rem   run-docker.cmd wp ...     run WP-CLI, e.g. run-docker.cmd wp turing queue status
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

set "DC=docker compose"
set "WP=%DC% run --rm --no-deps cli wp"

if /i "%~1"=="stop"  ( %DC% --profile multisite --profile demo --profile fresh down & exit /b %ERRORLEVEL% )
if /i "%~1"=="reset" ( %DC% --profile multisite --profile demo --profile fresh down -v & exit /b %ERRORLEVEL% )
if /i "%~1"=="multisite" goto :multisite
if /i "%~1"=="demo" goto :demo
if /i "%~1"=="fresh" goto :fresh
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
    %WP% core install --url=http://localhost:%WP_PORT% --title="Viglet Turing test" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
    %WP% plugin activate viglet-turing || exit /b 1
) else (
    %WP% plugin activate viglet-turing >nul 2>&1
)

rem The SN site is converged on every start; a key is minted only while the plugin has none.
set "KEY="
for /f "delims=" %%k in ('%WP% option pluck viglet_turing_settings api_key 2^>nul') do set "KEY=%%k"
if "%KEY%"=="" (
    set "NEWKEY="
    for /f "delims=" %%k in ('powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_SITE%" -Locale "%TURING_LOCALE%"') do set "NEWKEY=%%k"
    call :setkey
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_SITE%" -Locale "%TURING_LOCALE%" -NoToken
)

echo.
echo WordPress:  http://localhost:%WP_PORT%/wp-admin   (admin / admin)
echo Plugin:     http://localhost:%WP_PORT%/wp-admin/admin.php?page=viglet-turing
echo Turing:     http://localhost:%TURING_PORT%   (admin / %TURING_ADMIN_PASSWORD%), SN site %SN_SITE%
echo WP-CLI:     run-docker.cmd wp turing queue status
exit /b 0

:setkey
if "%NEWKEY%"=="" ( echo Turing was not provisioned; the plugin has no API key yet. & exit /b 0 )
%WP% option patch update viglet_turing_settings api_key "%NEWKEY%" >nul
echo The plugin has an API key for the local Turing.
exit /b 0

:demo
rem The demo sites side by side (D283, D325), each a WordPress and an SN site of its own beside the
rem browser tests' site (D219), sharing its Turing. Each is described by demo/sites/<key>.json
rem (theme, seed, title, WooCommerce, port, SN site, language), read by bin\demo-site.mjs: en
rem (Tarnfell) and pt (Recanto da Serra) are the stores, edu (Lumen School) and edu-pt (Escola
rem Lumen) the school. `demo` sets up both stores, `demo <key>` one site, and `demo reset <key>`
rem deletes only the one named, so recording one never costs another its orders and searches.
set "RESET="
set "WHICH=%~2"
if /i "%WHICH%"=="reset" (
    set "RESET=1"
    set "WHICH=%~3"
)
if "%WHICH%"=="" (
    if defined RESET ( echo Name the site to reset: run-docker.cmd demo reset en, pt, edu or edu-pt & exit /b 1 )
    call "%~f0" demo en || exit /b 1
    call "%~f0" demo pt || exit /b 1
    exit /b 0
)
set "D_NAME="
for /f "usebackq tokens=1,* delims==" %%a in (`node bin\demo-site.mjs "%WHICH%"`) do set "D_%%a=%%b"
if not defined D_NAME exit /b 1
set "SFX=%D_SFX%"
set "PORT=%D_PORT%"
set "SITE=%D_SITE%"
set "LOCALE=%D_LOCALE%"
if defined RESET (
    %DC% --profile demo rm -s -f wordpress-demo%SFX% cron-demo%SFX% db-demo%SFX%
    docker volume rm viglet-turing-wp_wp-demo%SFX% viglet-turing-wp_db-demo%SFX%
    exit /b 0
)
rem The single site and Turing first: the demo shares that Turing.
call "%~f0" || exit /b 1
set "WPD=%DC% run --rm --no-deps cli-demo%SFX% wp"
set "DEMOURL=http://localhost:%PORT%"
%DC% --profile demo up -d wordpress-demo%SFX% cron-demo%SFX% || exit /b 1

echo Waiting for the demo WordPress (%D_NAME%, %LOCALE%)...
set /a TRIES=0
:waitdemo
set /a TRIES+=1
%WPD% db check >nul 2>&1 && goto :readydemo
if %TRIES% geq 60 ( echo The demo WordPress did not come up. See: run-docker.cmd logs wordpress-demo%SFX% & exit /b 1 )
timeout /t 2 /nobreak >nul
goto :waitdemo

:readydemo
%WPD% core is-installed >nul 2>&1
if errorlevel 1 (
    echo Installing the demo WordPress...
    %WPD% core install --url=%DEMOURL% --title="%D_TITLE%" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
)
%WPD% plugin activate viglet-turing >nul 2>&1

set "KEY="
for /f "delims=" %%k in ('%WPD% option pluck viglet_turing_settings api_key 2^>nul') do set "KEY=%%k"
if "%KEY%"=="" (
    for /f "delims=" %%k in ('powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SITE%" -Locale "%LOCALE%"') do set "KEY=%%k"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SITE%" -Locale "%LOCALE%" -NoToken
)
if "%KEY%"=="" ( echo Turing was not provisioned for %SITE%; the demo has no API key. & exit /b 1 )
rem The key, the SN site and the language it indexes and searches in (D272, D274), as the site's
rem own settings.
%WPD% eval "$s = get_option( 'viglet_turing_settings', array() ); $s['api_key'] = '%KEY%'; $s['site_name'] = '%SITE%'; $s['locale'] = '%LOCALE%'; update_option( 'viglet_turing_settings', $s );" || exit /b 1

%WPD% theme activate %D_THEME% || exit /b 1
rem The site speaks its language, and the seed writes its content in it (D217).
if /i not "%LOCALE%"=="en_US" %WPD% language core install %LOCALE% >nul 2>&1
%WPD% site switch-language %LOCALE% || exit /b 1
%WPD% rewrite structure "/%%postname%%/" --hard || exit /b 1
rem A shop: WooCommerce on, before the catalog seed (which runs demo/setup.php first).
if defined D_WOO (
    %WPD% plugin is-installed woocommerce >nul 2>&1 || %WPD% plugin install woocommerce || exit /b 1
    %WPD% plugin activate woocommerce || exit /b 1
    if /i not "%LOCALE%"=="en_US" %WPD% language plugin install woocommerce %LOCALE% >nul 2>&1
)
%WPD% eval-file %D_SEED% || exit /b 1
rem The seed sets the shop's addresses in its language (D266), after WooCommerce registered them.
%WPD% rewrite flush --hard || exit /b 1
rem The seed makes its posts indexable after saving them, so nothing queued them: send them now.
%WPD% turing reindex || exit /b 1
rem The site's search history, once the content it finds is indexed (D269).
if defined D_HISTORY (
    %WPD% eval-file %D_HISTORY% || exit /b 1
)

echo.
echo Demo site %D_NAME% (%LOCALE%): %DEMOURL%/   (admin / admin), SN site %SITE%
echo Demo WP-CLI:     docker compose run --rm --no-deps cli-demo%SFX% wp ...
exit /b 0

:fresh
rem A WordPress the plugin was never installed on (D281): the store is seeded once, by WP-CLI with
rem src/ mounted, and then that copy is deactivated and everything it stored deleted. Its web
rem container never had the plugin, so the developer video installs the release zip in wp-admin.
if "%WP_FRESH_PORT%"=="" set "WP_FRESH_PORT=38084"
if /i "%~2"=="reset" (
    %DC% --profile fresh rm -s -f wordpress-fresh cron-fresh db-fresh
    docker volume rm viglet-turing-wp_wp-fresh viglet-turing-wp_db-fresh
    exit /b 0
)
rem The single site and Turing first: the fresh store connects to that Turing.
call "%~f0" || exit /b 1
set "WPF=%DC% run --rm --no-deps cli-fresh wp"
%DC% --profile fresh up -d wordpress-fresh cron-fresh || exit /b 1

echo Waiting for the fresh WordPress...
set /a TRIES=0
:waitfresh
set /a TRIES+=1
%WPF% db check >nul 2>&1 && goto :readyfresh
if %TRIES% geq 60 ( echo The fresh WordPress did not come up. See: run-docker.cmd logs wordpress-fresh & exit /b 1 )
timeout /t 2 /nobreak >nul
goto :waitfresh

:readyfresh
%WPF% core is-installed >nul 2>&1
if not errorlevel 1 goto :freshready
echo Installing the fresh WordPress and its catalog...
%WPF% core install --url=http://localhost:%WP_FRESH_PORT% --title="Tarnfell" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
%WPF% theme activate tarnfell || exit /b 1
%WPF% rewrite structure "/%%postname%%/" --hard || exit /b 1
%WPF% plugin install woocommerce --activate || exit /b 1
%WPF% plugin activate viglet-turing || exit /b 1
%WPF% eval-file /demo/seed/catalog.php || exit /b 1
%WPF% plugin deactivate viglet-turing || exit /b 1
%WPF% eval-file /demo/seed/forget-plugin.php || exit /b 1
%WPF% rewrite flush --hard || exit /b 1

:freshready
echo.
echo Fresh store:  http://localhost:%WP_FRESH_PORT%/wp-admin   (admin / admin), no plugin installed
echo Fresh WP-CLI: docker compose run --rm --no-deps cli-fresh wp ...
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
    %WPMS% core multisite-install --url=%MSURL% --title="Viglet Turing network" --admin_user=admin --admin_password=admin --admin_email=admin@example.org --skip-email || exit /b 1
    %DC% run --rm --no-deps --entrypoint sh cli-ms -c "cp /dumont/multisite.htaccess /var/www/html/.htaccess" || exit /b 1
    %WPMS% site create --slug=second --title="Second site" --email=admin@example.org || exit /b 1
)
%WPMS% plugin activate viglet-turing --network >nul 2>&1

rem Each site of the network holds its own key, bound to its site key (D190), so neither can
rem reach the other's documents. A key without a partition provisions the SN site's fields
rem once, since a partitioned key may not change the schema every site shares.
set "SETUP=powershell -NoProfile -ExecutionPolicy Bypass -File bin\turing-setup.ps1 -Base http://localhost:%TURING_PORT% -Password "%TURING_ADMIN_PASSWORD%" -Site "%SN_MS_SITE%" -Locale "%TURING_LOCALE%""
set "KEY="
for /f "delims=" %%k in ('%WPMS% option pluck viglet_turing_settings api_key 2^>nul') do set "KEY=%%k"
if not "%KEY%"=="" (
    %SETUP% -NoToken
    goto :networkready
)
for /f "delims=" %%k in ('%SETUP%') do set "KEY=%%k"
if "%KEY%"=="" ( echo Turing was not provisioned for %SN_MS_SITE%; the network has no API key. & exit /b 1 )
call :storekey %MSURL%/ %KEY% || exit /b 1
echo Provisioning the network's fields...
%WPMS% --url=%MSURL%/ turing reindex >nul || exit /b 1
for %%u in (%MSURL%/ %MSURL%/second/) do call :partitionkey %%u || exit /b 1

:networkready

echo.
echo Network:    %MSURL%/wp-admin/network/   (admin / admin), sites / and /second/, SN site %SN_MS_SITE%
exit /b 0

:storekey
rem %1 the site URL, %2 the key it stores.
%WPMS% --url=%1 eval "$s = get_option( 'viglet_turing_settings', array() ); $s['api_key'] = '%2'; update_option( 'viglet_turing_settings', $s );"
exit /b %ERRORLEVEL%

:partitionkey
rem %1 the site URL: mints a key bound to that site's key and stores it there.
set "SITEKEY="
for /f "delims=" %%s in ('%WPMS% --url^=%1 eval "echo ( new Viglet_Turing_Indexer() )->site_key();"') do set "SITEKEY=%%s"
if "%SITEKEY%"=="" ( echo %1 has no site key. & exit /b 1 )
set "PKEY="
for /f "delims=" %%k in ('%SETUP% -TokenOnly -PartitionValue "%SITEKEY%"') do set "PKEY=%%k"
if "%PKEY%"=="" ( echo Turing minted no key for %SITEKEY%. & exit /b 1 )
echo %1 holds a key bound to site_key %SITEKEY%.
call :storekey %1 %PKEY%
exit /b %ERRORLEVEL%

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
