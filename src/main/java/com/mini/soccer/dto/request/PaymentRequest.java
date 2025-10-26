package com.mini.soccer.dto.request;

import com.mini.soccer.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    @Size(max = 20, message = "Transaction code must be at most 20 characters")
    private String transactionCode;
}
