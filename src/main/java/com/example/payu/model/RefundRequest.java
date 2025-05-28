package com.example.payu.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequest {
    private String description;
    private String amount;
}
