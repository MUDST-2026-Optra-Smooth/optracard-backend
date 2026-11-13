[CmdletBinding()]
param(
    [string]$ApiBaseUrl = 'http://localhost:8080',
    [Parameter(Mandatory)] [string]$AdminEmail,
    [Parameter(Mandatory)] [string]$CardCornerSellerEmail,
    [Parameter(Mandatory)] [string]$RareFindsSellerEmail
)

$ErrorActionPreference = 'Stop'
$base = $ApiBaseUrl.TrimEnd('/')

function Convert-SecureStringToPlainText([securestring]$Value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

function Login([string]$Email, [string]$Label) {
    $securePassword = Read-Host "Password for $Label ($Email)" -AsSecureString
    $payload = @{ email = $Email; password = Convert-SecureStringToPlainText $securePassword } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body $payload
    if ([string]::IsNullOrWhiteSpace($response.token)) { throw "Login did not return a token for $Label." }
    return $response.token
}

function Invoke-Api([string]$Path, [string]$Method = 'Get', [string]$Token, $Body = $null) {
    $parameters = @{ Uri = "$base$Path"; Method = $Method; ErrorAction = 'Stop' }
    if ($Token) { $parameters.Headers = @{ Authorization = "Bearer $Token" } }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 8
    }
    return Invoke-RestMethod @parameters
}

function Find-Product($Products, [string]$Name, [string]$Owner) {
    $product = @($Products | Where-Object { $_.name -eq $Name }) | Select-Object -First 1
    if ($null -eq $product) { throw "Could not find '$Name' in $Owner inventory." }
    if ($null -eq $product.stock -or $product.stock -lt 1) { throw "'$Name' has no stock available for auction." }
    return $product
}

function New-AuctionPayload($Product, $Games, [string]$Title, [double]$StartingPrice) {
    $game = @($Games | Where-Object { $_.name -eq $Product.game }) | Select-Object -First 1
    if ($null -eq $game) { throw "Could not find card game '$($Product.game)' for '$Title'." }

    $now = Get-Date
    $evidence = @()
    if (-not [string]::IsNullOrWhiteSpace($Product.imageUrl)) { $evidence += $Product.imageUrl }
    return @{
        title = $Title
        gameId = $game.id
        type = $Product.type
        imageUrl = $Product.imageUrl
        description = $Product.description
        startingPrice = $StartingPrice
        startsAt = $now.AddMinutes(-1).ToString('yyyy-MM-ddTHH:mm:ss')
        endsAt = $now.AddHours(72).ToString('yyyy-MM-ddTHH:mm:ss')
        productId = $Product.id
        cardCondition = 'Near Mint'
        grading = 'Ungraded'
        evidenceImages = $evidence
    }
}

$adminToken = Login $AdminEmail 'administrator'
$existing = @(Invoke-Api '/api/auctions' 'Get' $null)
if ($existing.Count -gt 0) {
    throw 'Auctions already exist. Run scripts/reset-local-auctions.ps1 -Apply before creating the three local demo auctions.'
}

$games = @(Invoke-Api '/api/admin/card-games' 'Get' $adminToken)
$officialProducts = @(Invoke-Api '/api/admin/products' 'Get' $adminToken)
$official = Find-Product $officialProducts 'Pikachu ex Special Illustration' 'Official Store'
$null = Invoke-Api '/api/admin/auctions' 'Post' $adminToken (New-AuctionPayload $official $games $official.name 790)

$cardCornerToken = Login $CardCornerSellerEmail 'Card Corner seller'
$cardCornerProducts = @(Invoke-Api '/api/seller/products' 'Get' $cardCornerToken)
$gengar = Find-Product $cardCornerProducts 'Gengar VMAX Alternate Art' 'Card Corner BKK'
$gengarAuction = Invoke-Api '/api/seller/auctions' 'Post' $cardCornerToken (New-AuctionPayload $gengar $games $gengar.name 3850)
$null = Invoke-Api "/api/seller/auctions/$($gengarAuction.id)/submit" 'Post' $cardCornerToken
$null = Invoke-Api "/api/admin/auctions/$($gengarAuction.id)/review" 'Put' $adminToken @{ approved = $true; note = 'Local demo auction approved.' }

$rareFindsToken = Login $RareFindsSellerEmail 'Rare Finds seller'
$rareFindsProducts = @(Invoke-Api '/api/seller/products' 'Get' $rareFindsToken)
$blueEyes = Find-Product $rareFindsProducts 'Blue-Eyes White Dragon Ghost Rare' 'Rare Finds TCG'
$blueEyesAuction = Invoke-Api '/api/seller/auctions' 'Post' $rareFindsToken (New-AuctionPayload $blueEyes $games $blueEyes.name 7000)
$null = Invoke-Api "/api/seller/auctions/$($blueEyesAuction.id)/submit" 'Post' $rareFindsToken
$null = Invoke-Api "/api/admin/auctions/$($blueEyesAuction.id)/review" 'Put' $adminToken @{ approved = $true; note = 'Local demo auction approved.' }

Write-Host 'Created three live local demo auctions. Each ends 72 hours after creation and reserves its linked product.'
