# =============================================================
#  Loads backend/.env into the current process and starts the
#  application with the "dev" profile.
#
#  Spring Boot does not read .env files by itself, so this script
#  does it. Run from the backend folder:   .\run-dev.ps1
# =============================================================
if (-not (Test-Path ".env")) {
    Write-Error "backend/.env not found. Copy .env.example to .env and fill it in."
    exit 1
}

Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
    }
}

Write-Host "Starting NER-SmartLogix-AI (dev profile) ..." -ForegroundColor Cyan
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
