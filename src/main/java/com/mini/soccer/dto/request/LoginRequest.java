package com.mini.soccer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String password;
}
