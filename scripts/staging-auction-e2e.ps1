param(
    [Parameter(Mandatory = $true)][string]$ApiBaseUrl,
    [Parameter(Mandatory = $true)][string]$SellerEmail,
    [Parameter(Mandatory = $true)][string]$SellerPassword,
    [Parameter(Mandatory = $true)][string]$AdminEmail,
    [Parameter(Mandatory = $true)][string]$AdminPassword,
    [Parameter(Mandatory = $true)][string]$BuyerEmail,
    [Parameter(Mandatory = $true)][string]$BuyerPassword,
    [Parameter(Mandatory = $true)][string]$RunId,
    [int]$GameId = 1,
    [int]$CheckoutAuctionId = 0
)

# This script is intentionally staging-only. It creates only run-scoped records
# and always uses the normal administrator cancellation API for cleanup.
$ErrorActionPreference = 'Stop'
$baseUrl = $ApiBaseUrl.TrimEnd('/')
$auctionId = $null

function Invoke-Api {
    param([string]$Method, [string]$Path, [hashtable]$Headers = @{}, [object]$Body = $null)
    $arguments = @{ Uri = "$baseUrl$Path"; Method = $Method; Headers = $Headers; UseBasicParsing = $true; ErrorAction = 'Stop' }
    if ($null -ne $Body) { $arguments.ContentType = 'application/json'; $arguments.Body = $Body | ConvertTo-Json -Compress }
    try {
        $response = Invoke-WebRequest @arguments
        return [pscustomobject]@{ StatusCode = [int]$response.StatusCode; Body = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null } }
    } catch {
        if ($_.Exception.Response) { throw "$Method $Path failed with HTTP $([int]$_.Exception.Response.StatusCode)" }
        throw
    }
}

function Login([string]$Email, [string]$Password) {
    $result = Invoke-Api 'POST' '/api/auth/login' @{} @{ email = $Email; password = $Password }
    if ($result.StatusCode -ne 200 -or [string]::IsNullOrWhiteSpace($result.Body.token)) { throw 'Staging login did not return a token.' }
    return @{ Authorization = "Bearer $($result.Body.token)" }
}

$sellerHeaders = Login $SellerEmail $SellerPassword
$adminHeaders = Login $AdminEmail $AdminPassword
$buyerHeaders = Login $BuyerEmail $BuyerPassword
$now = [DateTime]::UtcNow
$startsAt = $now.AddMinutes(-1).ToString('yyyy-MM-ddTHH:mm:ss')
$endsAt = $now.AddDays(2).ToString('yyyy-MM-ddTHH:mm:ss')

try {
    $create = Invoke-Api 'POST' '/api/seller/auctions' $sellerHeaders @{
        title = "E2E-$RunId-auction"; gameId = $GameId; type = 'Single'; imageUrl = '';
        description = 'Resettable staging auction fixture'; startingPrice = 500; startsAt = $startsAt; endsAt = $endsAt
    }
    if ($create.StatusCode -notin 200, 201 -or $null -eq $create.Body.id) { throw 'Staging auction creation did not return an ID.' }
    $auctionId = [int]$create.Body.id

    if ((Invoke-Api 'POST' "/api/seller/auctions/$auctionId/submit" $sellerHeaders).StatusCode -ne 200) { throw 'Seller auction submission failed.' }
    $review = Invoke-Api 'PUT' "/api/admin/auctions/$auctionId/review" $adminHeaders @{ approved = $true; note = '' }
    if ($review.StatusCode -ne 200 -or $review.Body.status -ne 'LIVE') { throw 'Admin auction approval did not produce LIVE status.' }
    $bid = Invoke-Api 'POST' "/api/auctions/$auctionId/bids" $buyerHeaders @{ amount = 500 }
    if ($bid.StatusCode -ne 200 -or $bid.Body.amount -ne 500) { throw 'Buyer bid was not accepted.' }

    # A normal auction cannot reach checkout in a fast test because its minimum duration is one hour.
    # Supply an already-ended fixture ID only when a staging reset job has prepared it.
    if ($CheckoutAuctionId -gt 0) {
        $checkout = Invoke-Api 'POST' "/api/auctions/$CheckoutAuctionId/checkout" $buyerHeaders @{
            recipientName = 'E2E Buyer'; recipientPhone = '0800000000'; shippingAddress = 'Bangkok'; shippingMethod = 'STANDARD'; paymentMethod = 'E2E'
        }
        if ($checkout.StatusCode -ne 200) { throw 'Prepared auction checkout failed.' }
        $settlements = Invoke-Api 'GET' '/api/seller/settlements?type=AUCTION_CHARGE' $sellerHeaders
        if ($settlements.StatusCode -ne 200) { throw 'Auction settlement report could not be read.' }
    }

    Write-Host "Staging auction journey passed for E2E-$RunId."
} finally {
    if ($null -ne $auctionId) {
        $cleanup = Invoke-Api 'PUT' "/api/admin/auctions/$auctionId/cancel" $adminHeaders @{ note = "E2E-$RunId cleanup" }
        if ($cleanup.StatusCode -ne 200) { throw 'Staging auction cleanup failed.' }
    }
}
