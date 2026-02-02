package com.pos.server.repository;

import com.pos.server.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    java.util.List<Expense> findAllByRestaurantId(String restaurantId);
}
