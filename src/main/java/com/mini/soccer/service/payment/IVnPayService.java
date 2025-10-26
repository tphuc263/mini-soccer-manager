package com.mini.soccer.service.payment;

import java.math.BigDecimal;
import java.util.Map;

public interface IVnPayService {

    String createPaymentUrl(BigDecimal amount,
                            String orderInfo,
                            String transactionRef,
                            String clientIp);

    boolean validateSignature(Map<String, String> params);
}
