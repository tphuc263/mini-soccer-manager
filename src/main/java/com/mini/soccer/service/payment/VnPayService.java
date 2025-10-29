package com.mini.soccer.service.payment;

import com.mini.soccer.config.VnPayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VnPayService implements IVnPayService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties properties;

    @Override
    public String createPaymentUrl(BigDecimal amount,
                                   String orderInfo,
                                   String transactionRef,
                                   String clientIp) {
        validateConfig();
        Map<String, String> params = buildBaseParams(amount, orderInfo, transactionRef, clientIp);
        params.put("vnp_SecureHashType", "HmacSHA512");

        Map<String, String> signingParams = params.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .filter(entry -> !"vnp_SecureHash".equalsIgnoreCase(entry.getKey()))
                .filter(entry -> !"vnp_SecureHashType".equalsIgnoreCase(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));

        String hashData = toQueryString(signingParams);
        String secureHash = hmacSHA512(properties.getHashSecret(), hashData);
        String queryString = toQueryString(params);
        return properties.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public boolean validateSignature(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String providedHash = params.get("vnp_SecureHash");
        if (isBlank(providedHash)) {
            return false;
        }

        Map<String, String> filtered = params.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> {
                    String key = entry.getKey();
                    return !"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));

        if (filtered.isEmpty()) {
            return false;
        }

        String hashData = toQueryString(filtered);
        String expectedHash = hmacSHA512(properties.getHashSecret(), hashData);
        return expectedHash.equalsIgnoreCase(providedHash);
    }

    private Map<String, String> buildBaseParams(BigDecimal amount,
                                                String orderInfo,
                                                String transactionRef,
                                                String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_Command", properties.getCommand());
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", toVnPayAmount(amount));
        params.put("vnp_CurrCode", properties.getCurrencyCode());
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", properties.getOrderType());
        params.put("vnp_Locale", properties.getLocale());
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", now.format(DATE_TIME_FORMATTER));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(DATE_TIME_FORMATTER));
        return params;
    }

    private String toQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String toVnPayAmount(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String hmacSHA512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte hashByte : hashBytes) {
                sb.append(String.format("%02x", hashByte));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign VNPay payload", e);
        }
    }

    private void validateConfig() {
        Map<String, String> required = Map.of(
                "payment.vnpay.tmnCode", properties.getTmnCode(),
                "payment.vnpay.hashSecret", properties.getHashSecret(),
                "payment.vnpay.payUrl", properties.getPayUrl(),
                "payment.vnpay.returnUrl", properties.getReturnUrl(),
                "payment.vnpay.version", properties.getVersion(),
                "payment.vnpay.command", properties.getCommand(),
                "payment.vnpay.currencyCode", properties.getCurrencyCode(),
                "payment.vnpay.locale", properties.getLocale(),
                "payment.vnpay.orderType", properties.getOrderType()
        );

        String missingKeys = required.entrySet().stream()
                .filter(entry -> isBlank(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

        if (!missingKeys.isEmpty()) {
            throw new IllegalStateException("VNPay configuration is incomplete. Please review settings for: " + missingKeys);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
