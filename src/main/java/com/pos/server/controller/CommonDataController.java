package com.pos.server.controller;

import com.pos.server.model.*;
import com.pos.server.service.CommonDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CommonDataController {

    @Autowired
    private CommonDataService dataService;

    // Customers
    @GetMapping("/customers")
    public List<Customer> getCustomers(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return dataService.getAllCustomers(restaurantId);
    }

    @PostMapping("/customers")
    public ResponseEntity<?> saveCustomer(@RequestBody Customer customer,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        if (restaurantId != null)
            customer.setRestaurantId(restaurantId);
        return ResponseEntity.ok(Map.of("ok", true, "customer", dataService.saveCustomer(customer)));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id) {
        dataService.deleteCustomer(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable String id, @RequestBody Customer customer,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        customer.setId(id);
        if (restaurantId != null)
            customer.setRestaurantId(restaurantId);
        return ResponseEntity.ok(Map.of("ok", true, "customer", dataService.saveCustomer(customer)));
    }

    // Expenses
    @GetMapping("/expenses")
    public List<Expense> getExpenses(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return dataService.getAllExpenses(restaurantId);
    }

    @PostMapping("/expenses")
    public ResponseEntity<?> saveExpense(@RequestBody Expense expense,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        if (restaurantId != null)
            expense.setRestaurantId(restaurantId);
        return ResponseEntity.ok(Map.of("ok", true, "expense", dataService.saveExpense(expense)));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable String id) {
        dataService.deleteExpense(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // Settings
    @GetMapping("/settings")
    public Settings getSettings(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return dataService.getSettings(restaurantId);
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Settings settings,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return ResponseEntity.ok(Map.of("ok", true, "settings", dataService.updateSettings(settings, restaurantId)));
    }

    // Credit Notes
    @GetMapping("/credit-notes")
    public List<CreditNote> getCreditNotes(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return dataService.getAllCreditNotes(restaurantId);
    }

    @PostMapping("/credit-notes")
    public ResponseEntity<?> saveCreditNote(@RequestBody CreditNote note,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        if (restaurantId != null)
            note.setRestaurantId(restaurantId);
        return ResponseEntity.ok(Map.of("ok", true, "note", dataService.saveCreditNote(note)));
    }

    @PutMapping("/credit-notes/{id}")
    public ResponseEntity<?> updateCreditNote(@PathVariable String id, @RequestBody CreditNote note) {
        return ResponseEntity.ok(Map.of("ok", true, "note", dataService.updateCreditNote(id, note)));
    }

    // Credit Transactions (Ledger)
    @GetMapping("/credit-transactions")
    public List<CreditTransaction> getCreditTransactions(@RequestParam(required = false) String mobile,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        if (mobile != null)
            return dataService.getTransactionsByMobile(mobile, restaurantId);
        return dataService.getAllCreditTransactions(restaurantId);
    }

    @PostMapping("/credit-transactions")
    public ResponseEntity<?> saveCreditTransaction(@RequestBody CreditTransaction tx,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        if (restaurantId != null)
            tx.setRestaurantId(restaurantId);
        return ResponseEntity.ok(Map.of("ok", true, "transaction", dataService.saveCreditTransaction(tx)));
    }

    @DeleteMapping("/credit-transactions/mobile/{mobile}")
    public ResponseEntity<?> deleteCreditTransactions(@PathVariable String mobile,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        dataService.deleteTransactionsByMobile(mobile, restaurantId);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
