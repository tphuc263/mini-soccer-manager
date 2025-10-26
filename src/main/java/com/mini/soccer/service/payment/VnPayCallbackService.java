package com.mini.soccer.service.payment;

import com.mini.soccer.dto.response.PaymentResponse;
import com.mini.soccer.enums.BookingStatus;
import com.mini.soccer.enums.PaymentStatus;
import com.mini.soccer.model.Booking;
import com.mini.soccer.model.Payment;
import com.mini.soccer.repository.BookingRepository;
import com.mini.soccer.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VnPayCallbackService {

    private static final DateTimeFormatter VNP_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final IVnPayService vnPayService;

    @Transactional
    public PaymentResponse handleCallback(Map<String, String> vnpParams) {
        if (!vnPayService.validateSignature(vnpParams)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid VNPay signature");
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing VNPay transaction reference");
        }

        Payment payment = locatePayment(txnRef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for transaction reference"));

        BigDecimal reportedAmount = parseAmount(vnpParams.get("vnp_Amount"));
        if (reportedAmount != null && payment.getAmount() != null && payment.getAmount().compareTo(reportedAmount) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount does not match VNPay callback");
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionStatus = vnpParams.get("vnp_TransactionStatus");

        payment.setVnpResponseCode(responseCode);
        payment.setVnpTxnRef(txnRef);

        String orderInfo = vnpParams.get("vnp_OrderInfo");
        if (orderInfo != null && !orderInfo.isBlank()) {
            payment.setVnpOrderInfo(orderInfo);
        }

        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        if (success) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(parsePayDate(vnpParams.get("vnp_PayDate")));
            payment.setRefundedAt(null);

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaidAt(null);
        }

        Payment saved = paymentRepository.save(payment);
        return toPaymentResponse(saved);
    }

    private Optional<Payment> locatePayment(String txnRef) {
        Optional<Payment> byVnpTxnRef = paymentRepository.findByVnpTxnRef(txnRef);
        if (byVnpTxnRef.isPresent()) {
            return byVnpTxnRef;
        }
        return paymentRepository.findByTransactionCode(txnRef);
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount).movePointLeft(2);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid VNPay amount format");
        }
    }

    private LocalDateTime parsePayDate(String payDate) {
        if (payDate == null || payDate.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(payDate, VNP_DATETIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.now();
        }
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        Booking booking = payment.getBooking();
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(booking != null ? booking.getBookingId() : null)
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .transactionCode(payment.getTransactionCode())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .paymentUrl(null)
                .vnpTxnRef(payment.getVnpTxnRef())
                .vnpOrderInfo(payment.getVnpOrderInfo())
                .vnpResponseCode(payment.getVnpResponseCode())
                .build();
    }
}
