<<<<<<< HEAD
# LearntriX Backend Runner (PowerShell)
# Loads environment variables from backend/.env and starts Spring Boot

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from $envFile"
    Get-Content $envFile | Where-Object { $_ -match '^\s*[^#=\s]+\s*=' } | ForEach-Object {
        $parts = $_ -split '=', 2
        $key = $parts[0].Trim()
        $val = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $val, [System.EnvironmentVariableTarget]::Process)
    }
} else {
    Write-Warning ".env file not found at $envFile. Using default application.yml configurations."
}

=======
# Starts the LearntriX backend with backend/.env loaded into the process environment.
#
# Spring Boot does not read .env files on its own, so without this launcher the
# database password and SMTP credentials never reach the application and outbound
# email silently does nothing.
#
#   cd backend
#   .\run.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $root '.env'

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        # Strip optional surrounding quotes so values with spaces work either way.
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
             ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        Set-Item -Path "env:$key" -Value $value
    }
    Write-Host "Loaded environment from $envFile" -ForegroundColor DarkGray
} else {
    Write-Host "No .env found at $envFile - falling back to application.yml defaults." -ForegroundColor Yellow
}

if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    Write-Host "ERROR: DB_PASSWORD is not set. application.yml would fall back to its placeholder" -ForegroundColor Red
    Write-Host "       default and Postgres would reject it with 'password authentication failed" -ForegroundColor Red
    Write-Host "       for user `"postgres`"'. Set DB_PASSWORD in backend/.env." -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($env:MAIL_USERNAME) -or [string]::IsNullOrWhiteSpace($env:MAIL_PASSWORD)) {
    Write-Host "WARNING: MAIL_USERNAME / MAIL_PASSWORD are empty - activation emails will NOT be delivered." -ForegroundColor Yellow
    Write-Host "         Fill them in backend/.env (Gmail needs a 16-char App Password)." -ForegroundColor Yellow
} else {
    Write-Host "SMTP: $($env:MAIL_HOST):$($env:MAIL_PORT) as $($env:MAIL_USERNAME)" -ForegroundColor DarkGray
}

Set-Location $root
>>>>>>> b72e728 (application updated)
mvn spring-boot:run
