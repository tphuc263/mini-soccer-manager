package com.mini.soccer.repository;

import com.mini.soccer.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking_BookingId(Long bookingId);
    boolean existsByTransactionCode(String transactionCode);
    Optional<Payment> findByTransactionCode(String transactionCode);
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    List<Payment> findByBooking_BookingIdIn(Collection<Long> bookingIds);
}
