package com.example.payu.dto;

import com.example.payu.model.Buyer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestTO {
    private String totalAmount;
    private Buyer buyer;
}
