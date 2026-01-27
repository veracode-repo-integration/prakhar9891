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
            cn.setId(UUID.randomUUID().toString());
        return creditNoteRepository.save(cn);
    }
}
