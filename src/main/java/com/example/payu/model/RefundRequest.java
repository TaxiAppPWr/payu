package com.example.payu.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequest {
    private String orderId;
    private String description;
    private String amount;
}
