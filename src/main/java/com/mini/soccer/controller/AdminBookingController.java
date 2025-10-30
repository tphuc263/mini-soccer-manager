package com.mini.soccer.controller;

import com.mini.soccer.dto.request.UpdatePaymentStatusRequest;
import com.mini.soccer.dto.response.AdminBookingDetailResponse;
import com.mini.soccer.dto.response.AdminBookingSummaryResponse;
import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.dto.response.PaymentResponse;
import com.mini.soccer.service.booking.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final IBookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminBookingSummaryResponse>>> getBookings(
            @RequestParam(name = "bookingCode", required = false) String bookingCode,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<AdminBookingSummaryResponse> bookings = bookingService.getAdminBookings(bookingCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Retrieved bookings"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> getBookingDetail(
            @PathVariable Long bookingId) {
        AdminBookingDetailResponse booking = bookingService.getAdminBookingDetail(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Retrieved booking detail"));
    }

    @PatchMapping("/{bookingId}/payment-status")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @PathVariable Long bookingId,
            @RequestBody @Valid UpdatePaymentStatusRequest request) {
        PaymentResponse payment = bookingService.updateBookingPaymentStatus(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment status updated successfully"));
    }
}
