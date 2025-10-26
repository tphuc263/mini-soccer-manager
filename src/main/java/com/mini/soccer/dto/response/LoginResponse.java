package com.mini.soccer.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginResponse {
    String accessToken;
    String tokenType;
    Long userId;
    String fullName;
    String phoneNumber;
    String role;
}
