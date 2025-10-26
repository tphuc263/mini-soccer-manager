package com.mini.soccer.controller;

import com.mini.soccer.dto.response.AdminBookingDetailResponse;
import com.mini.soccer.dto.response.AdminBookingSummaryResponse;
import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.service.booking.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final IBookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBookingSummaryResponse>>> getBookings(
            @RequestParam(name = "bookingCode", required = false) String bookingCode) {
        List<AdminBookingSummaryResponse> bookings = bookingService.getAdminBookings(bookingCode);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Retrieved bookings"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> getBookingDetail(
            @PathVariable Long bookingId) {
        AdminBookingDetailResponse booking = bookingService.getAdminBookingDetail(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Retrieved booking detail"));
    }
}
