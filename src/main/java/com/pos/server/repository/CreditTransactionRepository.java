package com.pos.server.repository;

import com.pos.server.model.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {
    List<CreditTransaction> findByMobile(String mobile);

    List<CreditTransaction> findByMobileAndRestaurantId(String mobile, String restaurantId);

    List<CreditTransaction> findAllByRestaurantId(String restaurantId);
}
