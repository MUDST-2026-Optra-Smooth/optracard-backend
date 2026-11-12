[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$ApiBaseUrl)
$ErrorActionPreference = 'Stop'
$base = $ApiBaseUrl.TrimEnd('/')
if (-not [Uri]::IsWellFormedUriString($base, [UriKind]::Absolute)) { throw 'Provide an absolute API URL.' }
$auctions = @(Invoke-RestMethod -Uri "$base/api/auctions" -Method Get)
foreach ($auction in $auctions) {
    if ($null -ne $auction.winningUserId -or $null -ne $auction.leadingUserId) { throw 'Public list exposed a private bidder ID.' }
}
if ($auctions.Count -gt 0) {
    $id = $auctions[0].id
    $detail = Invoke-RestMethod -Uri "$base/api/auctions/$id" -Method Get
    if ($null -ne $detail.winningUserId -or $null -ne $detail.leadingUserId) { throw 'Public detail exposed a private bidder ID.' }
    $null = Invoke-RestMethod -Uri "$base/api/auctions/$id/bids" -Method Get
} else {
    Write-Host 'Public list is empty: detail and bid-history checks were not exercised.'
}
foreach ($path in @('/api/superadmin/auctions', '/api/auction-account/notifications')) {
    $status = 200
    try { $null = Invoke-WebRequest -Uri "$base$path" -Method Get -UseBasicParsing }
    catch { if ($null -eq $_.Exception.Response) { throw }; $status = [int]$_.Exception.Response.StatusCode }
    if ($status -notin @(401,403)) { throw "Anonymous access was not denied for $path (status $status)." }
}
Write-Host 'Auction read-only smoke passed. No bids, orders or moderation actions were created.'
