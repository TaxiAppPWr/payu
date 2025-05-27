package com.example.payu.model;

import java.math.BigDecimal;

public class RefundRequest {
    private String description="Refund";
    private String amount;  // Zmieniamy typ na BigDecimal, żeby obsługiwać liczby

    public String getDescription() {
        return description;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
