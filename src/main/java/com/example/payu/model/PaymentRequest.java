package com.example.payu.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    private String description;
    private String currencyCode;
    private String totalAmount;
    private Buyer buyer;
    private String customerIp;
    private String merchantPosId;
    private String continueUrl;
}
