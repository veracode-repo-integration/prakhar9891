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
    public List<Customer> getCustomers() {
        return dataService.getAllCustomers();
    }

    @PostMapping("/customers")
    public ResponseEntity<?> saveCustomer(@RequestBody Customer customer) {
        return ResponseEntity.ok(Map.of("ok", true, "customer", dataService.saveCustomer(customer)));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id) {
        dataService.deleteCustomer(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable String id, @RequestBody Customer customer) {
        customer.setId(id);
        return ResponseEntity.ok(Map.of("ok", true, "customer", dataService.saveCustomer(customer)));
    }

    // Expenses
    @GetMapping("/expenses")
    public List<Expense> getExpenses() {
        return dataService.getAllExpenses();
    }

    @PostMapping("/expenses")
    public ResponseEntity<?> saveExpense(@RequestBody Expense expense) {
        return ResponseEntity.ok(Map.of("ok", true, "expense", dataService.saveExpense(expense)));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable String id) {
        dataService.deleteExpense(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // Settings
    @GetMapping("/settings")
    public Settings getSettings() {
        return dataService.getSettings();
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Settings settings) {
        return ResponseEntity.ok(Map.of("ok", true, "settings", dataService.updateSettings(settings)));
    }

    // Credit Notes
    @GetMapping("/credit-notes")
    public List<CreditNote> getCreditNotes() {
        return dataService.getAllCreditNotes();
    }

    @PostMapping("/credit-notes")
    public ResponseEntity<?> saveCreditNote(@RequestBody CreditNote note) {
        return ResponseEntity.ok(Map.of("ok", true, "note", dataService.saveCreditNote(note)));
    }

    @PutMapping("/credit-notes/{id}")
    public ResponseEntity<?> updateCreditNote(@PathVariable String id, @RequestBody CreditNote note) {
        return ResponseEntity.ok(Map.of("ok", true, "note", dataService.updateCreditNote(id, note)));
    }
}
