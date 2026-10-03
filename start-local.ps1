# =============================================================
#  Start the whole NER-SmartLogix-AI stack on localhost.
#
#  No Docker: PostgreSQL 17 + PostGIS runs from E:\workspace\pgsql17 against the
#  cluster in E:\workspace\pgdata. See docs/localhost-setup.md.
#
#  Usage:   .\start-local.ps1
#           .\start-local.ps1 -Stop        (shut everything down)
#           .\start-local.ps1 -Build       (rebuild the frontend first)
# =============================================================
param(
    [switch]$Stop,
    [switch]$Build
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

# ---------------------------------------------------------------- 3. frontend
if ($Build) {
    '     rebuilding the frontend bundle'
    Push-Location (Join-Path $Root 'frontend')
    npm run build
    Pop-Location
}

"3/3  Frontend on $frontendPort"
$viteLog = Join-Path $LogDir 'frontend.log'
Remove-Item $viteLog -ErrorAction SilentlyContinue
# The dev server gives hot reload; it needs roughly 400 MB, so on a machine that is
# short of memory swap `vite` for `vite preview` (serves the built dist/ instead).
Start-Process -FilePath 'C:\Program Files\nodejs\npx.cmd' `
    -ArgumentList 'vite', '--port', "$frontendPort", '--strictPort' `
    -WorkingDirectory (Join-Path $Root 'frontend') `
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
'================================================================'
"Open   http://localhost:$frontendPort"
"Logs   $LogDir"
'Stop   .\start-local.ps1 -Stop'
