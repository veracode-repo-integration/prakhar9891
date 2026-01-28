# Backend Test Suite - POS System
# Usage: ./backend_test_suite.ps1

$BaseUrl = "http://localhost:4000/api"
$TestsFailed = 0
$TestsPassed = 0

function Assert-Success ($Response, $TestName) {
    if ($Response) {
        Write-Output "PASS: $TestName" 
        $script:TestsPassed++
    }
    else {
        Write-Output "FAIL: $TestName"
        $script:TestsFailed++
    }
}

function Assert-Error ($Block, $TestName) {
    try {
        & $Block
        Write-Output "FAIL: $TestName (Expected error)"
        $script:TestsFailed++
    }
    catch {
        Write-Output "PASS: $TestName (Got expected error)"
        $script:TestsPassed++
    }
}

Write-Output "`nStarting Backend Test Suite...`n"

# ------------------------------------------------------------------------------------------------
# 1. Product Tests
# ------------------------------------------------------------------------------------------------
Write-Output "--- Product Tests ---"
try {
    $Product = @{ name = "TestProd_Auto"; price = 150; stock = 50 } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/pos-products" -Method Post -Body $Product -ContentType "application/json"
    Assert-Success ($Res.id -and $Res.name -eq "TestProd_Auto") "Create Product"

    if ($Res.id) {
        $ProdId = $Res.id
        # Update
        $Update = @{ price = 160 } | ConvertTo-Json
        $ResUp = Invoke-RestMethod -Uri "$BaseUrl/pos-products/$ProdId" -Method Put -Body $Update -ContentType "application/json"
        Assert-Success ($ResUp.ok -eq $true) "Update Product"
    }
    else {
        Write-Output "SKIP: Update Product (Create failed)"
        $script:TestsFailed++
    }
}
catch {
    Write-Output "FAIL: Product Flow Exception: $_"
    $script:TestsFailed++
}


# ------------------------------------------------------------------------------------------------
# 2. Table Tests (Complex JSON)
# ------------------------------------------------------------------------------------------------
Write-Output "`n--- Table Tests ---"
try {
    # Create
    $Table = @{ name = "TestTable_Auto"; capacity = 6 } | ConvertTo-Json
    $Res = Invoke-RestMethod -Uri "$BaseUrl/tables" -Method Post -Body $Table -ContentType "application/json"
    $TableId = $Res.table.id
    Assert-Success ($TableId) "Create Table"

    if ($TableId) {
        # Update with Array (The critical fix test)
        $OrderItems = @(@{ id = "p1"; qty = 2 }, @{ id = "p2"; qty = 1 })
        $Update = @{ status = "occupied"; currentOrder = $OrderItems } | ConvertTo-Json -Depth 5
        
        $ResUp = Invoke-RestMethod -Uri "$BaseUrl/tables/$TableId" -Method Put -Body $Update -ContentType "application/json"
        Assert-Success ($ResUp.ok -eq $true -and $ResUp.table.status -eq "occupied") "Update Table with Array (JSON Fix)"
        
        # Verify persistence of JSON string
        $GetTable = Invoke-RestMethod -Uri "$BaseUrl/tables" -Method Get
        $Verify = $GetTable | Where-Object { $_.id -eq $TableId }
        Assert-Success ($Verify.currentOrder -match "p1") "Verify Table Order Persistence"
    }
}
catch {
    Write-Output "FAIL: Table Flow Exception: $_"
    $script:TestsFailed++
}


# ------------------------------------------------------------------------------------------------
# 3. Checkout Tests (The "Impossible" Error Fixes)
# ------------------------------------------------------------------------------------------------
Write-Output "`n--- Checkout Tests ---"
try {
    # Case A: User as String "admin"
    $CheckoutA = @{
        items       = @(@{ id = "prod_checkout"; qty = 1; price = 100 });
        paymentInfo = @{ method = "cash"; customer = @{ name = "Guest" } };
        user        = "admin_string"
    } | ConvertTo-Json -Depth 5

    $ResA = Invoke-RestMethod -Uri "$BaseUrl/checkout" -Method Post -Body $CheckoutA -ContentType "application/json"
    Assert-Success ($ResA.ok -eq $true) "Checkout (User as String)"

    # Case B: User as Object { username: "admin" }
    $CheckoutB = @{
        items       = @(@{ id = "prod_checkout"; qty = 1; price = 100 });
        paymentInfo = @{ method = "cash"; customer = @{ name = "Guest" } };
        user        = @{ username = "admin_obj"; role = "admin" }
    } | ConvertTo-Json -Depth 5

    $ResB = Invoke-RestMethod -Uri "$BaseUrl/checkout" -Method Post -Body $CheckoutB -ContentType "application/json"
    Assert-Success ($ResB.ok -eq $true) "Checkout (User as Object)"
    
    # Case C: Validating Response Structure (Fix for Receipt Screen)
    Assert-Success ($ResB.order -and $ResB.order.items) "Checkout Response Has Order Object"

}
catch {
    Write-Output "FAIL: Checkout Flow Exception: $_"
    $script:TestsFailed++
}


# ------------------------------------------------------------------------------------------------
# 4. Settings & Customer
# ------------------------------------------------------------------------------------------------
Write-Output "`n--- Settings & Customer Tests ---"
try {
    # Customer
    $Cust = @{ name = "Test Cust"; mobile = "9876543210" } | ConvertTo-Json
    $ResC = Invoke-RestMethod -Uri "$BaseUrl/customers" -Method Post -Body $Cust -ContentType "application/json"
    Assert-Success ($ResC.ok -eq $true) "Create Customer"

    # Settings
    $Set = @{ merchantName = "Auto Test Shop" } | ConvertTo-Json
    $ResS = Invoke-RestMethod -Uri "$BaseUrl/settings" -Method Put -Body $Set -ContentType "application/json"
    Assert-Success ($ResS.ok -eq $true -and $ResS.settings.merchantName -eq "Auto Test Shop") "Update Settings"

}
catch {
    Write-Output "FAIL: Settings/Customer Exception: $_"
    $script:TestsFailed++
}

# Summary
Write-Output "`n--------------------------------------------------"
Write-Output "SUMMARY: passed: $script:TestsPassed | failed: $script:TestsFailed"
if ($script:TestsFailed -eq 0) {
    Write-Output "ALL BACKEND TESTS PASSED"
}
else {
    Write-Output "SOME TESTS FAILED"
}
