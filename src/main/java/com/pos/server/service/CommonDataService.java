package com.pos.server.service;

import com.pos.server.model.*;
import com.pos.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CommonDataService {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ExpenseRepository expenseRepository;
    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private CreditNoteRepository creditNoteRepository;
    @Autowired
    private CreditTransactionRepository creditTransactionRepository;
    @Autowired
    private InvoiceSequenceRepository invoiceSequenceRepository;

    public java.util.Optional<Customer> getCustomerByMobile(String mobile, String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            System.err.println("[CommonDataService] Customer lookup failed: Restaurant ID is missing");
            return java.util.Optional.empty();
        }
        String rid = restaurantId.toLowerCase().trim();
        System.out.println("[CommonDataService] Looking up customer by mobile: " + mobile + " for restaurant: " + rid);
        return customerRepository.findByMobileAndRestaurantId(mobile, rid);
    }

    // Customers
    public List<Customer> getAllCustomers(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty())
            return new java.util.ArrayList<>();
        return customerRepository.findAllByRestaurantId(restaurantId.toLowerCase().trim());
    }

    public Customer saveCustomer(Customer c) {
        if (c.getId() == null)
            c.setId(UUID.randomUUID().toString());
        if (c.getRestaurantId() != null) {
            c.setRestaurantId(c.getRestaurantId().toLowerCase().trim());
        }
        System.out.println("[CommonDataService] Saving customer: " + c.getName() + " with mobile: " + c.getMobile()
                + " for restaurant: " + c.getRestaurantId());
        return customerRepository.save(c);
    }

    public void deleteCustomer(String id) {
        customerRepository.deleteById(id);
    }

    // Expenses
    public List<Expense> getAllExpenses(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty())
            return new java.util.ArrayList<>();
        return expenseRepository.findAllByRestaurantId(restaurantId);
    }

    public Expense saveExpense(Expense e) {
        if (e.getId() == null)
            e.setId(UUID.randomUUID().toString());
        return expenseRepository.save(e);
    }

    public void deleteExpense(String id) {
        expenseRepository.deleteById(id);
    }

    // Settings
    public Settings getSettings(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return new Settings();
        }
        try {
            return settingsRepository.findByRestaurantId(restaurantId).orElseGet(() -> {
                Settings s = new Settings();
                s.setRestaurantId(restaurantId);
                s.setId(UUID.randomUUID().toString());
                return s;
            });
        } catch (Exception e) {
            System.err.println("[Emergency] Settings DB failure for " + restaurantId + ": " + e.getMessage());
            Settings fallback = new Settings();
            fallback.setRestaurantId(restaurantId);
            return fallback;
        }
    }

    public Settings updateSettings(Settings s, String restaurantId) {
        if (restaurantId != null && !restaurantId.trim().isEmpty()) {
            String rid = restaurantId.toLowerCase().trim();
            s.setRestaurantId(rid);
            try {
                // Ensure we update the existing one for this restaurant if it exists
                settingsRepository.findByRestaurantId(rid).ifPresent(existing -> s.setId(existing.getId()));
                return settingsRepository.save(s);
            } catch (Exception e) {
                System.err.println("[Emergency] Settings Update failure: " + e.getMessage());
                // Silently fail or return the passed object to prevent 500
                return s;
            }
        } else {
            throw new RuntimeException("Restaurant ID required for settings update");
        }
    }

    public synchronized String getNextInvoiceNumber(String restaurantId) {
        if (restaurantId == null)
            return "INV-" + UUID.randomUUID().toString().substring(0, 8);
        String rid = restaurantId.toLowerCase().trim();
        InvoiceSequence seq = invoiceSequenceRepository.findById(rid).orElseGet(() -> {
            InvoiceSequence s = new InvoiceSequence();
            s.setRestaurantId(rid);
            s.setLastValue(0);
            return s;
        });

        long nextVal = seq.getLastValue() + 1;
        seq.setLastValue(nextVal);
        invoiceSequenceRepository.save(seq);

        Settings settings = getSettings(rid);
        String prefix = settings.getInvoicePrefix() != null ? settings.getInvoicePrefix() : "INV-2025-";

        return prefix + String.format("%04d", nextVal);
    }

    // Credit Notes
    public List<CreditNote> getAllCreditNotes(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty())
            return new java.util.ArrayList<>();
        return creditNoteRepository.findAllByRestaurantId(restaurantId);
    }

    public CreditNote saveCreditNote(CreditNote cn) {
        if (cn.getId() == null)
            cn.setId("CN-" + UUID.randomUUID().toString().substring(0, 8));
        return creditNoteRepository.save(cn);
    }

    public CreditNote updateCreditNote(String id, CreditNote updates) {
        CreditNote existing = creditNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit Note not found"));

        if (updates.getCustomerId() != null)
            existing.setCustomerId(updates.getCustomerId());
        if (updates.getCustomerName() != null)
            existing.setCustomerName(updates.getCustomerName());
        if (updates.getMobile() != null)
            existing.setMobile(updates.getMobile());
        if (updates.getAmount() != 0)
            existing.setAmount(updates.getAmount());
        if (updates.getReason() != null)
            existing.setReason(updates.getReason());
        if (updates.getCreatedAt() != null)
            existing.setCreatedAt(updates.getCreatedAt());
        if (updates.getUsedAt() != null)
            existing.setUsedAt(updates.getUsedAt());
        if (updates.getDate() != 0)
            existing.setDate(updates.getDate());
        existing.setUsed(updates.isUsed());

        return creditNoteRepository.save(existing);
    }

    // Credit Ledger (Transactions)
    public List<CreditTransaction> getAllCreditTransactions(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty())
            return new java.util.ArrayList<>();
        return creditTransactionRepository.findAllByRestaurantId(restaurantId);
    }

    public List<CreditTransaction> getTransactionsByMobile(String mobile, String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty())
            return new java.util.ArrayList<>();
        return creditTransactionRepository.findByMobileAndRestaurantId(mobile, restaurantId);
    }

    public CreditTransaction saveCreditTransaction(CreditTransaction tx) {
        if (tx.getId() == null)
            tx.setId(UUID.randomUUID().toString());
        return creditTransactionRepository.save(tx);
    }

    public void deleteTransactionsByMobile(String mobile, String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty())
            return;
        List<CreditTransaction> txs = creditTransactionRepository.findByMobileAndRestaurantId(mobile, restaurantId);
        creditTransactionRepository.deleteAll(txs);
    }
}
