# =============================================================
#  Start the whole NER-SmartLogix-AI stack on localhost.
#
#  No Docker: PostgreSQL 17 + PostGIS runs from E:\workspace\pgsql17 against the
#  cluster in E:\workspace\pgdata. See docs/localhost-setup.md.
#
#  Usage:   .\start-local.ps1                 dev server: hot reload, no service worker
#           .\start-local.ps1 -Preview        serves dist/: service worker active,
#                                             no hot reload. Implies -Build unless
#                                             dist/ already exists.
#           .\start-local.ps1 -Preview -Build force a rebuild, then serve dist/
#           .\start-local.ps1 -Build          rebuild, then run the dev server
#           .\start-local.ps1 -Stop           shut everything down
#
#  Why two modes: Vite's dev server deliberately does not serve the generated
#  sw.js, because module-level HMR and service-worker caching fight each other.
#  Offline *queuing* works in both modes (that is IndexedDB), but an offline cold
#  load of the app shell only works under -Preview.
# =============================================================
param(
    [switch]$Stop,
    [switch]$Build,
    [switch]$Preview
)

$ErrorActionPreference = 'Stop'

$Root     = $PSScriptRoot
$PgHome   = 'E:\workspace\pgsql17'
$PgData   = 'E:\workspace\pgdata'
$Mvn      = 'C:\Users\admin\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd'
$LogDir   = Join-Path $Root 'logs'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Read-DotEnv([string]$path) {
    if (-not (Test-Path $path)) { throw "Missing $path - copy .env.example and fill it in." }
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
        }
    }
}

function Stop-OnPort([int]$port, [string]$label) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        try {
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction Stop
            "  stopped $label (PID $($c.OwningProcess))"
        } catch { "  could not stop PID $($c.OwningProcess)" }
    }
}

# ---------------------------------------------------------------- stop
if ($Stop) {
    'Stopping the stack...'
    Stop-OnPort 5174 'frontend'
    Stop-OnPort 8081 'backend'
    & "$PgHome\bin\pg_ctl.exe" -D $PgData -m fast stop 2>$null | Out-Null
    '  stopped postgres'
    'Done.'
    exit 0
}

# ---------------------------------------------------------------- env
Read-DotEnv (Join-Path $Root 'backend\.env')
$backendPort  = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { '8080' }
$frontendPort = 5174

# ---------------------------------------------------------------- 1. database
'1/3  PostgreSQL + PostGIS'
& "$PgHome\bin\pg_isready.exe" -h localhost -p 5432 -q
if ($LASTEXITCODE -ne 0) {
    Start-Process -FilePath "$PgHome\bin\pg_ctl.exe" `
        -ArgumentList '-D', $PgData, '-l', "$PgData\server.log", '-o', '"-p 5432"', 'start' `
        -WindowStyle Hidden
    # An unclean shutdown makes the server replay WAL before it accepts connections.
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 3
        & "$PgHome\bin\pg_isready.exe" -h localhost -p 5432 -q
        if ($LASTEXITCODE -eq 0) { break }
    }
}
& "$PgHome\bin\pg_isready.exe" -h localhost -p 5432
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL did not come up - see E:\workspace\pgdata\server.log' }

# ---------------------------------------------------------------- 2. backend
# Re-running this script must not start a second backend against a port that is
# already taken, so an instance that is already healthy is left alone.
$backendUp = $false
try {
    Invoke-WebRequest "http://localhost:$backendPort/actuator/health" -TimeoutSec 3 `
        -UseBasicParsing | Out-Null
    $backendUp = $true
} catch { }

if ($backendUp) {
    "2/3  Spring Boot backend on $backendPort - already running, left alone"
} else {
    "2/3  Spring Boot backend on $backendPort"
    $env:MAVEN_OPTS = '-Xms64m -Xmx512m -XX:+UseSerialGC'
    $backendLog = Join-Path $LogDir 'backend.log'
    Remove-Item $backendLog -ErrorAction SilentlyContinue
    Start-Process -FilePath $Mvn `
        -ArgumentList '-B', 'spring-boot:run', '-Dspring-boot.run.profiles=dev' `
        -WorkingDirectory (Join-Path $Root 'backend') `
        -RedirectStandardOutput $backendLog `
        -RedirectStandardError (Join-Path $LogDir 'backend.err.log') `
        -WindowStyle Hidden

    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 3
        if (Test-Path $backendLog) {
            $text = Get-Content $backendLog -Raw -ErrorAction SilentlyContinue
            if ($text -match 'Started NerSmartLogixApplication') { break }
            if ($text -match 'APPLICATION FAILED TO START|BUILD FAILURE') {
                Get-Content $backendLog -Tail 25
                throw 'Backend failed to start.'
            }
        }
    }
}

# ---------------------------------------------------------------- 3. frontend
$frontendDir = Join-Path $Root 'frontend'
$distIndex   = Join-Path $frontendDir 'dist\index.html'

# -Preview needs a bundle to serve, so build if one is missing or a build was asked for.
$needsBuild = $Build -or ($Preview -and -not (Test-Path $distIndex))
if ($needsBuild) {
    '     building the frontend bundle'
    Push-Location $frontendDir
    npm run build
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'Frontend build failed.' }
    Pop-Location
}

$mode = if ($Preview) { 'preview (service worker active, no hot reload)' }
        else { 'dev server (hot reload, no service worker)' }
"3/3  Frontend on $frontendPort - $mode"

$viteLog = Join-Path $LogDir 'frontend.log'
Remove-Item $viteLog -ErrorAction SilentlyContinue

# Unlike the backend, the frontend is always restarted: the requested mode may differ
# from whatever is currently serving, and that switch is the whole point of -Preview.
Stop-OnPort $frontendPort 'previous frontend'

# `vite preview` is a plain static server over dist/. It serves the generated sw.js,
# which the dev server deliberately does not, and uses a fraction of the memory.
$viteArgs = if ($Preview) {
    @('vite', 'preview', '--port', "$frontendPort", '--strictPort')
} else {
    @('vite', '--port', "$frontendPort", '--strictPort')
}

Start-Process -FilePath 'C:\Program Files\nodejs\npx.cmd' `
    -ArgumentList $viteArgs `
    -WorkingDirectory $frontendDir `
    -RedirectStandardOutput $viteLog `
    -RedirectStandardError (Join-Path $LogDir 'frontend.err.log') `
    -WindowStyle Hidden

for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 2
    try { Invoke-WebRequest "http://localhost:$frontendPort" -TimeoutSec 2 -UseBasicParsing | Out-Null; break }
    catch { }
}

# ---------------------------------------------------------------- report
''
'================================================================'
& "$PgHome\bin\pg_isready.exe" -h localhost -p 5432
foreach ($svc in @(
    @{ n = 'backend '; u = "http://localhost:$backendPort/actuator/health" },
    @{ n = 'frontend'; u = "http://localhost:$frontendPort" })) {
    try {
        $r = Invoke-WebRequest $svc.u -TimeoutSec 5 -UseBasicParsing
        "$($svc.n)  $($svc.u) -> HTTP $($r.StatusCode)"
    } catch { "$($svc.n)  $($svc.u) -> DOWN" }
}

# Under -Preview the generated service worker should be reachable. If it is not, the
# offline shell will silently not work, so say so here rather than let it be assumed.
if ($Preview) {
    try {
        $sw = Invoke-WebRequest "http://localhost:$frontendPort/sw.js" -TimeoutSec 5 -UseBasicParsing
        # A status check alone would lie here: Vite answers an unknown path with
        # index.html, so a missing sw.js still returns 200 - as HTML. The content type
        # is what actually distinguishes a real worker from the SPA fallback.
        $type = $sw.Headers['Content-Type']
        if ($type -match 'javascript') {
            "service worker  /sw.js -> HTTP $($sw.StatusCode), $type ($($sw.RawContentLength) bytes)"
        } else {
            "service worker  /sw.js -> served as $type, NOT JavaScript."
            "                The bundle is missing; re-run with -Preview -Build."
        }
    } catch {
        "service worker  /sw.js -> NOT SERVED (offline shell will not work)"
    }
}
'================================================================'
"Mode   $mode"
"Open   http://localhost:$frontendPort"
"Logs   $LogDir"
'Stop   .\start-local.ps1 -Stop'
if ($Preview) {
    'Note   test the offline shell in Chrome or Edge: DevTools > Application >'
    '       Service Workers, then Network > Offline and hard-refresh.'
} else {
    'Note   no service worker in this mode. Use -Preview to test offline loading.'
}
