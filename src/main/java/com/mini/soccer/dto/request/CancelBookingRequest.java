package com.mini.soccer.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelBookingRequest {

    @Size(max = 255)
    private String reason;
}
