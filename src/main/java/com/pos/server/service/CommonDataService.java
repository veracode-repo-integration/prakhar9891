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
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
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
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
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
    public Settings getSettings() {
        return settingsRepository.findById("default").orElse(new Settings());
    }

    public Settings updateSettings(Settings s) {
        s.setId("default");
        return settingsRepository.save(s);
    }

    // Credit Notes
    public List<CreditNote> getAllCreditNotes() {
        return creditNoteRepository.findAll();
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
    public List<CreditTransaction> getAllCreditTransactions() {
        return creditTransactionRepository.findAll();
    }

    public List<CreditTransaction> getTransactionsByMobile(String mobile) {
        return creditTransactionRepository.findByMobile(mobile);
    }

    public CreditTransaction saveCreditTransaction(CreditTransaction tx) {
        if (tx.getId() == null)
            tx.setId(UUID.randomUUID().toString());
        return creditTransactionRepository.save(tx);
    }

    public void deleteTransactionsByMobile(String mobile) {
        List<CreditTransaction> txs = creditTransactionRepository.findByMobile(mobile);
        creditTransactionRepository.deleteAll(txs);
    }
}
