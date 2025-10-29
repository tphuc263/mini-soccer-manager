package com.mini.soccer.controller;

import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.dto.response.PaymentResponse;
import com.mini.soccer.service.payment.VnPayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/payments/vnpay")
@RequiredArgsConstructor
public class VnPayCallbackController {

    private final VnPayCallbackService callbackService;
    @Value("${frontend.vnpay.callback-url:}")
    private String frontendCallbackUrl;

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam Map<String, String> queryParams,
            @RequestHeader(value = "Accept", required = false) String acceptHeader
    ) {
        PaymentResponse payment = callbackService.handleCallback(queryParams);
        String message = userFacingMessage(payment);
        if (shouldReturnJson(queryParams, acceptHeader) || !StringUtils.hasText(frontendCallbackUrl)) {
            return ResponseEntity.ok(ApiResponse.success(payment, message));
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(frontendCallbackUrl);
        queryParams.forEach((key, value) -> {
            if (value != null) {
                uriBuilder.queryParam(key, value);
            }
        });

        if (StringUtils.hasText(message)) {
            uriBuilder.queryParam("message", message);
        }

        if (payment != null) {
            if (payment.getBookingId() != null) {
                uriBuilder.queryParam("bookingId", payment.getBookingId());
            }
            if (StringUtils.hasText(payment.getStatus())) {
                uriBuilder.queryParam("paymentStatus", payment.getStatus());
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriBuilder.build().toUri());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(@RequestBody Map<String, String> payload) {
        Map<String, String> params = payload != null ? new HashMap<>(payload) : Map.of();
        PaymentResponse payment = callbackService.handleCallback(params);
        return ResponseEntity.ok(ApiResponse.success(payment, userFacingMessage(payment)));
    }

    private String userFacingMessage(PaymentResponse payment) {
        if (payment == null) {
            return "Đã tiếp nhận phản hồi từ VNPay.";
        }
        return "PAID".equalsIgnoreCase(payment.getStatus())
                ? "Thanh toán VNPay thành công."
                : "Giao dịch VNPay chưa hoàn tất. Vui lòng kiểm tra lại hoặc liên hệ hỗ trợ.";
    }

    private boolean shouldReturnJson(Map<String, String> queryParams, String acceptHeader) {
        String format = queryParams.get("format");
        if (format != null && "json".equalsIgnoreCase(format)) {
            return true;
        }
        return acceptHeader != null && acceptHeader.contains("application/json");
    }
}
