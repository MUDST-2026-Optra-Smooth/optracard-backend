[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$ContainerName = 'optracard_db',
    [string]$Database = 'optracard',
    [string]$DatabaseUser = 'admin',
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'

if (-not $Apply) {
    Write-Host 'No changes were made. Re-run with -Apply after confirming the local database container.'
    Write-Host 'This resets auction rows, auction bids/events, and auction checkout orders only.'
    exit 0
}

$root = Split-Path -Parent $PSScriptRoot
$sqlFiles = @(
    'database/auction-migration.sql',
    'database/auction-completion-migration.sql',
    'database/reset-local-auctions.sql'
)

foreach ($relativePath in $sqlFiles) {
    $sqlPath = Join-Path $root $relativePath
    if ($PSCmdlet.ShouldProcess("$ContainerName/$Database", "Apply $relativePath")) {
        Get-Content -Raw $sqlPath | docker exec -i $ContainerName psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database
    }
}

Write-Host 'Auction reset completed. Products, stores and user accounts were retained.'
