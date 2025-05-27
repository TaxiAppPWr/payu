package com.example.payu.components;

import org.springframework.stereotype.Component;

@Component
public class PaymentData {
    private String paymentUrl = "https://secure.snd.payu.com/api/v2_1/orders/";
    private static final String BASE_URL = "https://secure.snd.payu.com/api/v2_1/orders";
    public String getRefundUrl(String orderId) {
        return BASE_URL + "/" + orderId + "/refunds";
    }
    public String getPaymentUrl() {
        return paymentUrl;
    }
}
