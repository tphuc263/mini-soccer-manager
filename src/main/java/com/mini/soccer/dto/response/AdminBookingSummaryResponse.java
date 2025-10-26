package com.mini.soccer.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class AdminBookingSummaryResponse {
    Long bookingId;
    String bookingCode;
    String status;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal totalAmount;
    LocalDateTime createdAt;
    Long userId;
    String userFullName;
    String userPhoneNumber;
    Long fieldId;
    String fieldName;
}
