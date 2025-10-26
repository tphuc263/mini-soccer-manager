package com.mini.soccer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnPayProperties {
    /**
     * Merchant terminal code provided by VNPay.
     */
    private String tmnCode;

    /**
     * Secret key used to sign requests.
     */
    private String hashSecret;

    /**
     * Endpoint to send customers for checkout.
     */
    private String payUrl;

    /**
     * URL that VNPay will redirect to after payment.
     */
    private String returnUrl;

    private String version;
    private String command;
    private String currencyCode;
    private String locale;
    private String orderType;
}
