package com.mini.soccer.controller;

import com.mini.soccer.dto.request.LoginRequest;
import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.dto.response.LoginResponse;
import com.mini.soccer.security.jwt.JwtUtils;
import com.mini.soccer.security.userdetails.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateAccessToken(authentication);
        String role = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(principal.getUserId())
                .fullName(principal.getFullName())
                .phoneNumber(principal.getPhoneNumber())
                .role(role)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
}
