package com.mini.soccer.dto.request;

import com.mini.soccer.enums.PaymentMethod;
import com.mini.soccer.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePaymentStatusRequest {

    @NotNull
    private PaymentStatus status;

    private PaymentMethod paymentMethod;
}
