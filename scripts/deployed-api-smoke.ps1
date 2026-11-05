param(
    [Parameter(Mandatory = $true)]
    [string]$ApiBaseUrl,
    [string]$Email,
    [string]$Password
)

$ErrorActionPreference = 'Stop'
$baseUrl = $ApiBaseUrl.TrimEnd('/')

function Invoke-SmokeRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $arguments = @{
        Uri = "$baseUrl$Path"
        Method = $Method
        Headers = $Headers
        UseBasicParsing = $true
        ErrorAction = 'Stop'
    }
    if ($null -ne $Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = ($Body | ConvertTo-Json -Compress)
    }

    try {
        $response = Invoke-WebRequest @arguments
        return [pscustomobject]@{ StatusCode = [int]$response.StatusCode; Content = $response.Content }
    } catch {
        $httpResponse = $_.Exception.Response
        if ($null -ne $httpResponse) {
            $reader = New-Object System.IO.StreamReader($httpResponse.GetResponseStream())
            $content = $reader.ReadToEnd()
            throw "${Method} ${Path} returned $([int]$httpResponse.StatusCode): $content"
        }
        throw
    }
}

function Assert-Status {
    param([object]$Response, [int]$Expected, [string]$Name)
    if ($Response.StatusCode -ne $Expected) {
        throw "$Name expected HTTP $Expected but received HTTP $($Response.StatusCode)"
    }
}

$health = Invoke-SmokeRequest -Method 'GET' -Path '/actuator/health'
Assert-Status $health 200 'Health endpoint'
if (($health.Content | ConvertFrom-Json).status -ne 'UP') {
    throw 'Health endpoint did not report UP'
}

$openApi = Invoke-SmokeRequest -Method 'GET' -Path '/v3/api-docs'
Assert-Status $openApi 200 'OpenAPI endpoint'
if (-not ($openApi.Content | ConvertFrom-Json).paths.'/api/products/search') {
    throw 'OpenAPI document did not include the public product search route'
}

$catalog = Invoke-SmokeRequest -Method 'GET' -Path '/api/products/catalog?page=0&pageSize=5'
Assert-Status $catalog 200 'Public catalog endpoint'
$search = Invoke-SmokeRequest -Method 'GET' -Path '/api/products/search?q=pokemon'
Assert-Status $search 200 'Public search endpoint'

if ($Email -and $Password) {
    $login = Invoke-SmokeRequest -Method 'POST' -Path '/api/auth/login' -Body @{ email = $Email; password = $Password }
    Assert-Status $login 200 'Read-only smoke login'
    $token = ($login.Content | ConvertFrom-Json).token
    if ([string]::IsNullOrWhiteSpace($token)) { throw 'Login response did not contain a token' }
    $headers = @{ Authorization = "Bearer $token" }
    Assert-Status (Invoke-SmokeRequest -Method 'GET' -Path '/api/profile' -Headers $headers) 200 'Read-only profile endpoint'
    Assert-Status (Invoke-SmokeRequest -Method 'GET' -Path '/api/orders' -Headers $headers) 200 'Read-only order history endpoint'
} else {
    Write-Host 'No E2E credentials supplied; authenticated read-only checks were skipped.'
}

Write-Host 'Deployed API smoke checks passed.'
