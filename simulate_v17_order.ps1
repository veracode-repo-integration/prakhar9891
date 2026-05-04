# Zomato Simulation - POS System (v17 Test)
# Usage: ./simulate_order.ps1

$BaseUrl = "https://pos-backend-981650451279.us-central1.run.app/api"
$RestaurantId = "divya"

Write-Output "Simulating incoming Zomato Webhook for RID: $RestaurantId ..."

$Payload = @{
    order = @{
        id = "ZMT-V17-$(Get-Random -Minimum 1000 -Maximum 9999)"
        platform = "ZOMATO"
        items = @(
            @{ id = "P1"; quantity = 2; name = "Elite V17 Test Item" }
        )
    }
} | ConvertTo-Json -Depth 5

$Headers = @{
    "X-Restaurant-Id" = $RestaurantId
}

try {
    $Response = Invoke-RestMethod -Uri "$BaseUrl/webhooks/orders" -Method Post -Body $Payload -ContentType "application/json" -Headers $Headers
    Write-Output "SUCCESS: Webhook Sent. Backend response: $($Response.status)"
    Write-Output "Check your phone for the v17 Nuclear Siren!"
}
catch {
    Write-Output "FAIL: Could not trigger webhook: $_"
}
