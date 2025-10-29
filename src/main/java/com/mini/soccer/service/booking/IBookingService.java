package com.mini.soccer.service.booking;

import com.mini.soccer.dto.request.BookingRequest;
import com.mini.soccer.dto.request.CancelBookingRequest;
import com.mini.soccer.dto.request.PaymentRequest;
import com.mini.soccer.dto.response.AdminBookingDetailResponse;
import com.mini.soccer.dto.response.AdminBookingSummaryResponse;
import com.mini.soccer.dto.response.BookingResponse;
import com.mini.soccer.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IBookingService {

    BookingResponse createBooking(BookingRequest request);

    BookingResponse cancelBooking(Long bookingId, CancelBookingRequest request);

    PaymentResponse payForBooking(Long bookingId, PaymentRequest request, String clientIp);

    Page<AdminBookingSummaryResponse> getAdminBookings(String bookingCode, Pageable pageable);

    AdminBookingDetailResponse getAdminBookingDetail(Long bookingId);

    List<BookingResponse> getCurrentUserBookings();
}
