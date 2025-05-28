package com.example.payu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RefundResponseWrapper {
    private String status;
    private Object refundResponse;
}
