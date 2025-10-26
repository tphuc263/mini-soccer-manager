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
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal pricePerHour;
    BigDecimal totalAmount;
    String status;
    LocalDateTime createdAt;
}
