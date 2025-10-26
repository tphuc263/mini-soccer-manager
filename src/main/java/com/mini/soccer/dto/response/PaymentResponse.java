package com.mini.soccer.dto.response;

import com.mini.soccer.enums.PaymentMethod;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class PaymentResponse {
    Long paymentId;
    Long bookingId;
    BigDecimal amount;
    String status;
    PaymentMethod paymentMethod;
    String transactionCode;
    LocalDateTime paidAt;
    LocalDateTime refundedAt;
    String paymentUrl;
    String vnpTxnRef;
    String vnpOrderInfo;
    String vnpResponseCode;
}
