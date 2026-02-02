package com.pos.server.repository;

import com.pos.server.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, String> {
    java.util.List<CreditNote> findAllByRestaurantId(String restaurantId);
}
