package com.mini.soccer.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class FieldResponse {
    Long fieldId;
    String name;
    BigDecimal pricePerHour;
    String description;
}
