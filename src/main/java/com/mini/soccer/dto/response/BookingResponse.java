package com.mini.soccer.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class BookingResponse {
    Long bookingId;
    String bookingCode;
    Long fieldId;
    String fieldName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal pricePerHour;
    BigDecimal totalAmount;
    String status;
    LocalDateTime createdAt;
    LocalDateTime cancelledAt;
    String cancellationReason;
    PaymentResponse payment;
}
