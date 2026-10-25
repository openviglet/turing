# Provisions the local Turing of docker-compose.yml for the plugin: waits for it, creates the SN
# site through /api/sn/manifest (the same call the plugin makes, so a rerun converges) and mints
# a developer API key. Prints the key on stdout and nothing else; progress goes to stderr.
#
# Called by run-docker.cmd. Local only: it refuses any URL that is not localhost.
param(
    [string]$Base = 'http://localhost:2700',
    [string]$Password = 'dumont-dev',
    [string]$Site = 'Sample',
    [string]$Locale = 'en_US',
    [switch]$NoToken
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
function Say([string]$text) { [Console]::Error.WriteLine($text) }

$Base = $Base.TrimEnd('/')
if ($Base -notmatch '^http://(localhost|127\.0\.0\.1)(:\d+)?$') {
    Say "Refusing to provision ${Base}: this script is for the local Turing only."
    exit 2
}

Say "Waiting for Turing on $Base (the first boot takes a minute or two)..."
$deadline = (Get-Date).AddMinutes(6)
while ($true) {
    try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 "$Base/api/discovery" | Out-Null; break }
    catch {
        if ((Get-Date) -gt $deadline) { Say 'Turing did not answer within 6 minutes. See: run-docker.cmd logs turing'; exit 1 }
        Start-Sleep -Seconds 3
    }
}

# Session login, as the console does: the session and XSRF cookies ride on every call.
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -UseBasicParsing -WebSession $session "$Base/api/discovery" | Out-Null
function Call([string]$Method, [string]$Path, $Body) {
    $headers = @{ Accept = 'application/json' }
    $xsrf = $session.Cookies.GetCookies($Base) | Where-Object Name -eq 'XSRF-TOKEN' | Select-Object -First 1
    if ($xsrf) { $headers['X-XSRF-TOKEN'] = $xsrf.Value }
    $request = @{ UseBasicParsing = $true; WebSession = $session; Method = $Method; Uri = "$Base$Path"; Headers = $headers }
    if ($null -ne $Body) {
        $request.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10 -Compress))
        $request.ContentType = 'application/json; charset=utf-8'
    }
    $response = Invoke-WebRequest @request
    if ($response.Content) { return $response.Content | ConvertFrom-Json }
    return $null
}

# /api/discovery answers before a fresh install has seeded its admin user, so the first logins
# can fail; retry for a while before giving up.
$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    try { Call 'POST' '/api/login' @{ username = 'admin'; password = $Password } | Out-Null; break }
    catch {
        if ((Get-Date) -gt $deadline) { Say "Login to Turing failed: $($_.Exception.Message)"; exit 1 }
        Start-Sleep -Seconds 3
    }
}

$engines = @(Call 'GET' '/api/se' $null)
$engine = @($engines | Where-Object { $_.enabled }) + $engines | Select-Object -First 1
if (-not $engine) { Say 'No search engine instance in this Turing.'; exit 1 }

# The site with the one field the plugin always declares; the plugin's own manifest adds the rest.
$manifest = @{
    name         = $Site
    description  = 'WordPress plugin (local development)'
    seInstanceId = $engine.id
    locales      = @($Locale)
    fields       = @(@{
        name = 'site_key'; type = 'STRING'; mandatory = $false; multiValued = $false
        facet = $true; facetName = @{ $Locale = 'Site' }
    })
}
Call 'POST' '/api/sn/manifest' $manifest | Out-Null
Say "SN site '$Site' ($Locale) is provisioned."

if ($NoToken) { exit 0 }
$token = (Call 'POST' '/api/dev/token' @{ title = 'viglet-dumont-wp'; description = 'WordPress plugin, local Docker' }).token
if (-not $token) { Say 'Turing answered no API key.'; exit 1 }
Write-Output $token
