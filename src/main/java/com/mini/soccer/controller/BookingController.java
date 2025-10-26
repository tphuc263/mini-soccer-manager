package com.mini.soccer.controller;

import com.mini.soccer.dto.request.BookingRequest;
import com.mini.soccer.dto.request.CancelBookingRequest;
import com.mini.soccer.dto.request.PaymentRequest;
import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.dto.response.BookingResponse;
import com.mini.soccer.dto.response.PaymentResponse;
import com.mini.soccer.service.booking.IBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final IBookingService bookingService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Booking created successfully"));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long bookingId,
                                                                      @Valid @RequestBody CancelBookingRequest request) {
        BookingResponse booking = bookingService.cancelBooking(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking cancelled successfully"));
    }

    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> payBooking(@PathVariable Long bookingId,
                                                                   @Valid @RequestBody PaymentRequest request,
                                                                   HttpServletRequest servletRequest) {
        PaymentResponse payment = bookingService.payForBooking(bookingId, request, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment completed successfully"));
    }
}
