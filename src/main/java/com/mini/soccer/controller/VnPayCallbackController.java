package com.mini.soccer.controller;

import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.dto.response.PaymentResponse;
import com.mini.soccer.service.payment.VnPayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/payments/vnpay")
@RequiredArgsConstructor
public class VnPayCallbackController {

    private final VnPayCallbackService callbackService;

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleCallback(@RequestParam Map<String, String> queryParams) {
        PaymentResponse payment = callbackService.handleCallback(queryParams);
        return ResponseEntity.ok(ApiResponse.success(payment, successMessage(payment)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(@RequestBody Map<String, String> payload) {
        Map<String, String> params = payload != null ? new HashMap<>(payload) : Map.of();
        PaymentResponse payment = callbackService.handleCallback(params);
        return ResponseEntity.ok(ApiResponse.success(payment, successMessage(payment)));
    }

    private String successMessage(PaymentResponse payment) {
        if (payment == null) {
            return "VNPay callback processed";
        }
        return "PAID".equalsIgnoreCase(payment.getStatus())
                ? "Payment confirmed successfully"
                : "Payment callback processed but payment is not completed yet";
    }
}
