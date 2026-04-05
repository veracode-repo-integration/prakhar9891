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

    @Autowired
    private com.pos.server.service.AuthService authService;

    // Customers
    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(dataService.getAllCustomers(rid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/customers/mobile/{mobile}")
    public ResponseEntity<?> getCustomerByMobile(@PathVariable String mobile,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return dataService.getCustomerByMobile(mobile, rid)
                    .map(c -> ResponseEntity.ok(Map.of("ok", true, "customer", c)))
                    .orElse(ResponseEntity.ok(Map.of("ok", false, "message", "Customer not found")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/customers")
    public ResponseEntity<?> saveCustomer(@RequestBody Customer customer,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            customer.setRestaurantId(rid);
            return ResponseEntity.ok(Map.of("ok", true, "customer", dataService.saveCustomer(customer)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            // Optionally check ownership here if needed
            dataService.deleteCustomer(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable String id, @RequestBody Customer customer,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            customer.setId(id);
            customer.setRestaurantId(rid);
            return ResponseEntity.ok(Map.of("ok", true, "customer", dataService.saveCustomer(customer)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    // Expenses
    @GetMapping("/expenses")
    public ResponseEntity<?> getExpenses(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(dataService.getAllExpenses(rid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/expenses")
    public ResponseEntity<?> saveExpense(@RequestBody Expense expense,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            expense.setRestaurantId(rid);
            return ResponseEntity.ok(Map.of("ok", true, "expense", dataService.saveExpense(expense)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            authService.validateRestaurantAccess(restaurantId);
            dataService.deleteExpense(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    // Credit Notes
    @GetMapping("/credit-notes")
    public ResponseEntity<?> getCreditNotes(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(dataService.getAllCreditNotes(rid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/credit-notes")
    public ResponseEntity<?> saveCreditNote(@RequestBody CreditNote note,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            note.setRestaurantId(rid);
            return ResponseEntity.ok(Map.of("ok", true, "note", dataService.saveCreditNote(note)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/credit-notes/{id}")
    public ResponseEntity<?> updateCreditNote(@PathVariable String id, @RequestBody CreditNote note,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            note.setRestaurantId(rid);
            return ResponseEntity.ok(Map.of("ok", true, "note", dataService.updateCreditNote(id, note)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    // Credit Transactions (Ledger)
    @GetMapping("/credit-transactions")
    public ResponseEntity<?> getCreditTransactions(@RequestParam(required = false) String mobile,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            if (mobile != null)
                return ResponseEntity.ok(dataService.getTransactionsByMobile(mobile, rid));
            return ResponseEntity.ok(dataService.getAllCreditTransactions(rid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/credit-transactions")
    public ResponseEntity<?> saveCreditTransaction(@RequestBody CreditTransaction tx,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            tx.setRestaurantId(rid);
            return ResponseEntity.ok(Map.of("ok", true, "transaction", dataService.saveCreditTransaction(tx)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/credit-transactions/mobile/{mobile}")
    public ResponseEntity<?> deleteCreditTransactions(@PathVariable String mobile,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            dataService.deleteTransactionsByMobile(mobile, rid);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
