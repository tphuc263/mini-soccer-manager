package com.mini.soccer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FieldRequest {

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private BigDecimal pricePerHour;

    private String description;
}
