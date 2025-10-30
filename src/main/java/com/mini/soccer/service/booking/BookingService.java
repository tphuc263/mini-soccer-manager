package com.mini.soccer.service.booking;

import com.mini.soccer.dto.request.BookingRequest;
import com.mini.soccer.dto.request.CancelBookingRequest;
import com.mini.soccer.dto.request.PaymentRequest;
import com.mini.soccer.dto.request.UpdatePaymentStatusRequest;
import com.mini.soccer.dto.response.AdminBookingDetailResponse;
import com.mini.soccer.dto.response.AdminBookingSummaryResponse;
import com.mini.soccer.dto.response.BookingResponse;
import com.mini.soccer.dto.response.PaymentResponse;
import com.mini.soccer.enums.BookingStatus;
import com.mini.soccer.enums.PaymentMethod;
import com.mini.soccer.enums.PaymentStatus;
import com.mini.soccer.enums.UserRole;
import com.mini.soccer.model.Booking;
import com.mini.soccer.model.Field;
import com.mini.soccer.model.Payment;
import com.mini.soccer.model.User;
import com.mini.soccer.repository.BookingRepository;
import com.mini.soccer.repository.FieldRepository;
import com.mini.soccer.repository.PaymentRepository;
import com.mini.soccer.repository.UserRepository;
import com.mini.soccer.security.userdetails.AppUserDetails;
import com.mini.soccer.service.payment.IVnPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService implements IBookingService {

    // Statuses that block a time slot from being booked again.
    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED
    );

    private final BookingRepository bookingRepository;
    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final IVnPayService vnPayService;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        User user = getAuthenticatedUser();

        Field field = fieldRepository.findById(request.getFieldId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found"));

        boolean overlap = bookingRepository.existsOverlappingBooking(
                field.getFieldId(),
                ACTIVE_BOOKING_STATUSES,
                request.getStartTime(),
                request.getEndTime()
        );
        if (overlap) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Time slot is already booked");
        }

        BigDecimal totalAmount = calculateTotalAmount(field.getPricePerHour(), request.getStartTime(), request.getEndTime());
        Booking booking = Booking.builder()
                .user(user)
                .field(field)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .priceAtBooking(field.getPricePerHour())
                .totalAmount(totalAmount)
                .bookingCode(generateBookingCode())
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved, null);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, CancelBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        AppUserDetails principal = getCurrentUserDetails();
        ensureOwnershipOrAdmin(booking.getUser().getUserId(), principal);

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking has already been cancelled");
        }
        if (!EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.PENDING).contains(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking cannot be cancelled in its current status");
        }
        if (!isAdmin(principal) && !booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel a booking that has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(normalizeCancellationReason(request.getReason()));
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        Payment updatedPayment = paymentRepository.findByBooking_BookingId(bookingId)
                .map(payment -> {
                    if (PaymentStatus.PAID.equals(payment.getStatus())) {
                        payment.setStatus(PaymentStatus.REFUND_PENDING);
                        payment.setRefundedAt(null);
                    } else if (PaymentStatus.REFUNDED.equals(payment.getStatus())) {
                        payment.setRefundedAt(payment.getRefundedAt());
                    } else if (PaymentStatus.REFUND_PENDING.equals(payment.getStatus())) {
                        payment.setRefundedAt(null);
                    }
                    return paymentRepository.save(payment);
                })
                .orElse(null);

        return toBookingResponse(booking, updatedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse payForBooking(Long bookingId, PaymentRequest request, String clientIp) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        ensureOwnershipOrAdmin(booking.getUser().getUserId());

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pay for a cancelled booking");
        }

        Payment payment = paymentRepository.findByBooking_BookingId(bookingId).orElse(null);
        if (payment != null && PaymentStatus.PAID.equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking has already been paid");
        }

        BigDecimal amount = request.getAmount() != null
                ? request.getAmount().setScale(2, RoundingMode.HALF_UP)
                : booking.getTotalAmount();

        if (amount.compareTo(booking.getTotalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must match booking total");
        }

        PaymentMethod paymentMethod = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.COD;

        String transactionCode = request.getTransactionCode();
        transactionCode = transactionCode != null && !transactionCode.isBlank()
                ? transactionCode.toUpperCase()
                : generateTransactionCode();

        if ((payment == null || !transactionCode.equalsIgnoreCase(payment.getTransactionCode()))
                && paymentRepository.existsByTransactionCode(transactionCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction code already exists");
        }

        Payment target = payment != null ? payment : new Payment();
        target.setBooking(booking);
        target.setAmount(amount);
        target.setPaymentMethod(paymentMethod);
        target.setTransactionCode(transactionCode);
        target.setRefundedAt(null);
        target.setVnpResponseCode(null);

        String paymentUrl = null;
        if (paymentMethod == PaymentMethod.VNPAY) {
            String orderInfo = "Thanh toan don dat san " + booking.getBookingCode();
            paymentUrl = vnPayService.createPaymentUrl(amount, orderInfo, transactionCode, normalizeClientIp(clientIp));
            target.setStatus(PaymentStatus.PENDING);
            target.setPaidAt(null);
            target.setVnpTxnRef(transactionCode);
            target.setVnpOrderInfo(orderInfo);
        } else {
            target.setStatus(PaymentStatus.PENDING);
            target.setPaidAt(null);
            target.setVnpTxnRef(null);
            target.setVnpOrderInfo(null);
        }

        Payment saved = paymentRepository.save(target);

        if (paymentMethod != PaymentMethod.VNPAY) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        return toPaymentResponse(saved, paymentUrl);
    }

    @Override
    @Transactional
    public PaymentResponse updateBookingPaymentStatus(Long bookingId, UpdatePaymentStatusRequest request) {
        PaymentStatus targetStatus = request.getStatus();
        if (targetStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target payment status is required");
        }
        AppUserDetails principal = getCurrentUserDetails();
        ensureAdmin(principal);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        Payment payment = paymentRepository.findByBooking_BookingId(bookingId).orElse(null);
        Payment updated = applyPaymentStatusUpdate(booking, payment, request);
        Payment saved = paymentRepository.save(updated);
        return toPaymentResponse(saved, null);
    }

    @Override
    public List<BookingResponse> getCurrentUserBookings() {
        AppUserDetails principal = getCurrentUserDetails();
        List<Booking> bookings = bookingRepository.findByUser_UserIdOrderByStartTimeDesc(principal.getUserId());
        List<Long> bookingIds = bookings.stream()
                .map(Booking::getBookingId)
                .toList();
        var payments = mapPaymentsByBookingId(bookingIds);
        return bookings.stream()
                .map(booking -> toBookingResponse(booking, payments.get(booking.getBookingId())))
                .toList();
    }

    @Override
    public Page<AdminBookingSummaryResponse> getAdminBookings(String bookingCode, Pageable pageable) {
        Pageable effectivePageable = ensureSort(pageable);
        Page<Booking> bookings;
        if (bookingCode != null && !bookingCode.isBlank()) {
            bookings = bookingRepository.findByBookingCodeContainingIgnoreCase(bookingCode.trim(), effectivePageable);
        } else {
            bookings = bookingRepository.findAll(effectivePageable);
        }
        return bookings.map(this::toAdminBookingSummary);
    }

    @Override
    public AdminBookingDetailResponse getAdminBookingDetail(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId).orElse(null);
        return toAdminBookingDetail(booking, payment);
    }

    private Pageable ensureSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private User getAuthenticatedUser() {
        AppUserDetails principal = getCurrentUserDetails();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void ensureOwnershipOrAdmin(Long ownerId) {
        AppUserDetails principal = getCurrentUserDetails();
        ensureOwnershipOrAdmin(ownerId, principal);
    }

    private void ensureOwnershipOrAdmin(Long ownerId, AppUserDetails principal) {
        if (isAdmin(principal)) {
            return;
        }
        if (!ownerId.equals(principal.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this booking");
        }
    }

    private AppUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof AppUserDetails principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }
        return principal;
    }

    private boolean isAdmin(AppUserDetails principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> UserRole.ADMIN.name().equals(role));
    }

    private void ensureAdmin(AppUserDetails principal) {
        if (!isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin privileges required");
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        return clientIp;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private BigDecimal calculateTotalAmount(BigDecimal pricePerHour, LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be greater than zero");
        }
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateBookingCode() {
        String code;
        do {
            code = "BK" + ThreadLocalRandom.current().nextInt(100000, 999999);
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }

    private String generateTransactionCode() {
        return "TX" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private BookingResponse toBookingResponse(Booking booking, Payment payment) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .fieldId(booking.getField().getFieldId())
                .fieldName(booking.getField().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .pricePerHour(booking.getPriceAtBooking())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .payment(payment != null ? toPaymentResponse(payment, null) : null)
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment payment, String paymentUrl) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking().getBookingId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .transactionCode(payment.getTransactionCode())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .paymentUrl(paymentUrl)
                .vnpTxnRef(payment.getVnpTxnRef())
                .vnpOrderInfo(payment.getVnpOrderInfo())
                .vnpResponseCode(payment.getVnpResponseCode())
                .build();
    }

    private AdminBookingSummaryResponse toAdminBookingSummary(Booking booking) {
        return AdminBookingSummaryResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .userId(booking.getUser().getUserId())
                .userFullName(booking.getUser().getFullName())
                .userPhoneNumber(booking.getUser().getPhoneNumber())
                .fieldId(booking.getField().getFieldId())
                .fieldName(booking.getField().getName())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .build();
    }

    private AdminBookingDetailResponse toAdminBookingDetail(Booking booking, Payment payment) {
        return AdminBookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .pricePerHour(booking.getPriceAtBooking())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .userId(booking.getUser().getUserId())
                .userFullName(booking.getUser().getFullName())
                .userPhoneNumber(booking.getUser().getPhoneNumber())
                .fieldId(booking.getField().getFieldId())
                .fieldName(booking.getField().getName())
                .fieldDescription(booking.getField().getDescription())
                .payment(payment != null ? toPaymentResponse(payment, null) : null)
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .build();
    }

    private Map<Long, Payment> mapPaymentsByBookingId(List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Payment> payments = paymentRepository.findByBooking_BookingIdIn(bookingIds);
        return payments.stream()
                .collect(Collectors.toMap(
                        payment -> payment.getBooking().getBookingId(),
                        payment -> payment,
                        (existing, replacement) -> replacement
                ));
    }

    private Payment applyPaymentStatusUpdate(Booking booking, Payment payment, UpdatePaymentStatusRequest request) {
        PaymentStatus target = request.getStatus();
        return switch (target) {
            case PENDING -> {
                Payment codPending = ensurePaymentEntity(payment, booking, request.getPaymentMethod());
                codPending.setStatus(PaymentStatus.PENDING);
                codPending.setPaidAt(null);
                codPending.setRefundedAt(null);
                yield codPending;
            }
            case PAID -> {
                Payment paidPayment = ensurePaymentEntity(payment, booking, request.getPaymentMethod());
                paidPayment.setStatus(PaymentStatus.PAID);
                if (paidPayment.getPaidAt() == null) {
                    paidPayment.setPaidAt(LocalDateTime.now());
                }
                paidPayment.setRefundedAt(null);
                yield paidPayment;
            }
            case REFUND_PENDING -> {
                Payment refundPending = requireExistingPayment(payment);
                refundPending.setStatus(PaymentStatus.REFUND_PENDING);
                refundPending.setRefundedAt(null);
                yield refundPending;
            }
            case REFUNDED -> {
                Payment refunded = requireExistingPayment(payment);
                refunded.setStatus(PaymentStatus.REFUNDED);
                if (refunded.getRefundedAt() == null) {
                    refunded.setRefundedAt(LocalDateTime.now());
                }
                yield refunded;
            }
        };
    }

    private Payment ensurePaymentEntity(Payment existing, Booking booking, PaymentMethod overrideMethod) {
        if (existing != null) {
            if (overrideMethod != null) {
                existing.setPaymentMethod(overrideMethod);
            }
            return existing;
        }
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentMethod(overrideMethod != null ? overrideMethod : PaymentMethod.COD);
        String transactionCode = generateTransactionCode();
        while (paymentRepository.existsByTransactionCode(transactionCode)) {
            transactionCode = generateTransactionCode();
        }
        payment.setTransactionCode(transactionCode);
        payment.setVnpTxnRef(null);
        payment.setVnpOrderInfo(null);
        payment.setVnpResponseCode(null);
        return payment;
    }

    private Payment requireExistingPayment(Payment payment) {
        if (payment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment record not found for booking");
        }
        return payment;
    }

    private String normalizeCancellationReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
