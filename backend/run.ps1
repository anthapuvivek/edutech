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

mvn spring-boot:run
