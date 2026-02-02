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

    // Customers
    public List<Customer> getAllCustomers(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty())
            return customerRepository.findAll();
        return customerRepository.findAllByRestaurantId(restaurantId);
    }

    public Customer saveCustomer(Customer c) {
        if (c.getId() == null)
            c.setId(UUID.randomUUID().toString());
        return customerRepository.save(c);
    }

    public void deleteCustomer(String id) {
        customerRepository.deleteById(id);
    }

    // Expenses
    public List<Expense> getAllExpenses(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty())
            return expenseRepository.findAll();
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
        if (restaurantId == null || restaurantId.isEmpty()) {
            return settingsRepository.findById("default").orElse(new Settings());
        }
        return settingsRepository.findByRestaurantId(restaurantId).orElseGet(() -> {
            Settings s = new Settings();
            s.setRestaurantId(restaurantId);
            s.setId(UUID.randomUUID().toString());
            return s;
        });
    }

    public Settings updateSettings(Settings s, String restaurantId) {
        if (restaurantId != null && !restaurantId.isEmpty()) {
            s.setRestaurantId(restaurantId);
            // Ensure we update the existing one for this restaurant if it exists
            settingsRepository.findByRestaurantId(restaurantId).ifPresent(existing -> s.setId(existing.getId()));
        } else {
            s.setId("default");
        }
        return settingsRepository.save(s);
    }

    // Credit Notes
    public List<CreditNote> getAllCreditNotes(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty())
            return creditNoteRepository.findAll();
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
        if (restaurantId == null || restaurantId.isEmpty())
            return creditTransactionRepository.findAll();
        return creditTransactionRepository.findAllByRestaurantId(restaurantId);
    }

    public List<CreditTransaction> getTransactionsByMobile(String mobile, String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty())
            return creditTransactionRepository.findByMobile(mobile);
        return creditTransactionRepository.findByMobileAndRestaurantId(mobile, restaurantId);
    }

    public CreditTransaction saveCreditTransaction(CreditTransaction tx) {
        if (tx.getId() == null)
            tx.setId(UUID.randomUUID().toString());
        return creditTransactionRepository.save(tx);
    }

    public void deleteTransactionsByMobile(String mobile, String restaurantId) {
        List<CreditTransaction> txs;
        if (restaurantId == null || restaurantId.isEmpty()) {
            txs = creditTransactionRepository.findByMobile(mobile);
        } else {
            txs = creditTransactionRepository.findByMobileAndRestaurantId(mobile, restaurantId);
        }
        creditTransactionRepository.deleteAll(txs);
    }
}
