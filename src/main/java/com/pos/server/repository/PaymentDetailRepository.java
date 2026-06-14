package com.pos.server.repository;

import com.pos.server.model.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Long> {
    List<PaymentDetail> findAllByReference(String reference);
}
