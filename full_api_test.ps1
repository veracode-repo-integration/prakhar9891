# Full API Test Suite
# Tests all endpoints defined in API_CONTRACT.md

$BaseUrl = "http://localhost:4000/api"
$ErrorActionPreference = "Continue"

function Test-Endpoint {
    param(
        [string]$Name,
        [scriptblock]$TestBlock
    )
    Write-Output "TESTing: $Name..."
    try {
        $Result = & $TestBlock
        if ($Result) {
            Write-Output " [PASS]"
            return $true
        }
        else {
            Write-Output " [FAIL]"
            return $false
        }
    }
    catch {
        Write-Output " [ERROR] $_"
        return $false
    }
}

$Global:Token = ""
$Global:ProductId = ""
$Global:TableId = ""
$Global:CustomerId = ""
$Global:HeldOrderId = ""

# ----------------- 1. Auth -----------------
Test-Endpoint "Auth: Register" {
    $Body = @{
        username  = "testuser_$(Get-Random)";
        password  = "password";
        email     = "test@test.com";
        firstName = "Test User";
        contact   = "1234567890";
        role      = "admin"
    } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/register" -Method Post -Body $Body -ContentType "application/json"
    return $Res.ok -eq $true
}

Test-Endpoint "Auth: Login" {
    $Body = @{ identifier = "admin"; passwordHash = "admin123" } | ConvertTo-Json
    try {
        $Res = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $Body -ContentType "application/json"
        $Global:Token = $Res.token
        return $Res.ok -eq $true
    }
    catch { return $false }
}

# ----------------- 2. Products -----------------
Test-Endpoint "Products: Create" {
    $Body = @{ name = "Test Product"; price = 100; stock = 10; category = "Test" } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/pos-products" -Method Post -Body $Body -ContentType "application/json"
    $Global:ProductId = $Res.id
    return $Res.id -ne $null
}

Test-Endpoint "Products: Get All" {
    $Res = Invoke-RestMethod -Uri "$BaseUrl/pos-products" -Method Get
    return $Res.Count -ge 1
}

Test-Endpoint "Products: Update" {
    $Body = @{ price = 150 } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/pos-products/$Global:ProductId" -Method Put -Body $Body -ContentType "application/json"
    return $Res.ok -eq $true
}

# ----------------- 3. Tables -----------------
Test-Endpoint "Tables: Create" {
    $Body = @{ name = "T_Test"; capacity = 4; area = "Test" } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/tables" -Method Post -Body $Body -ContentType "application/json"
    $Global:TableId = $Res.table.id
    return $Res.ok -eq $true
}

Test-Endpoint "Tables: Update Status (Array)" {
    $Items = @(@{ id = "p1"; qty = 1 })
    $Body = @{ status = "occupied"; currentOrder = $Items } | ConvertTo-Json -Depth 5
    $Res = Invoke-RestMethod -Uri "$BaseUrl/tables/$Global:TableId" -Method Put -Body $Body -ContentType "application/json"
    return $Res.ok -eq $true
}

# ----------------- 4. Customers -----------------
Test-Endpoint "Customers: Create" {
    $Body = @{ name = "Test Cust"; mobile = "9999999999" } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/customers" -Method Post -Body $Body -ContentType "application/json"
    $Global:CustomerId = $Res.customer.id
    return $Res.ok -eq $true
}

Test-Endpoint "Customers: Get All" {
    $Res = Invoke-RestMethod -Uri "$BaseUrl/customers" -Method Get
    return $Res.Count -ge 1
}

# ----------------- 5. Settings -----------------
Test-Endpoint "Settings: Get" {
    $Res = Invoke-RestMethod -Uri "$BaseUrl/settings" -Method Get
    return $Res.merchantName -ne $null
}

Test-Endpoint "Settings: Update" {
    $Body = @{ merchantName = "Updated Shop Name" } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/settings" -Method Put -Body $Body -ContentType "application/json"
    return $Res.ok -eq $true
}

# ----------------- 6. Held Orders -----------------
Test-Endpoint "Held Orders: Create" {
    $Body = @{
        items       = @(@{ id = "p1"; qty = 1; price = 100 });
        paymentInfo = @{ method = "cash" };
        user        = @{ username = "admin"; role = "admin" }
    } | ConvertTo-Json -Depth 5
    $Res = Invoke-RestMethod -Uri "$BaseUrl/held-orders" -Method Post -Body $Body -ContentType "application/json"
    $Global:HeldOrderId = $Res.heldOrder.id
    return $Res.ok -eq $true
}

Test-Endpoint "Held Orders: Get All" {
    $Res = Invoke-RestMethod -Uri "$BaseUrl/held-orders" -Method Get
    return $Res.Count -ge 1
}

# ----------------- 7. Checkout -----------------
Test-Endpoint "Checkout: Process" {
    $Body = @{
        items       = @(@{ id = "p1"; qty = 1; price = 100 });
        paymentInfo = @{ method = "cash"; customer = @{ name = "Walk-in" } };
        user        = @{ username = "admin"; role = "admin" }
    } | ConvertTo-Json -Depth 5
    $Res = Invoke-RestMethod -Uri "$BaseUrl/checkout" -Method Post -Body $Body -ContentType "application/json"
    return $Res.ok -eq $true
}

# Cleanup
if ($Global:ProductId) { Invoke-RestMethod -Uri "$BaseUrl/pos-products/$Global:ProductId" -Method Delete | Out-Null }
if ($Global:TableId) { Invoke-RestMethod -Uri "$BaseUrl/tables/$Global:TableId" -Method Delete | Out-Null }
if ($Global:CustomerId) { Invoke-RestMethod -Uri "$BaseUrl/customers/$Global:CustomerId" -Method Delete | Out-Null }
if ($Global:HeldOrderId) { Invoke-RestMethod -Uri "$BaseUrl/held-orders/$Global:HeldOrderId" -Method Delete | Out-Null }

Write-Host "`nTest Suite Completed."
