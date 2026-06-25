package com.bonaca.backend.payment.repository;

import com.bonaca.backend.payment.model.PaymentEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    boolean existsByRazorpayEventId(String razorpayEventId);
}
