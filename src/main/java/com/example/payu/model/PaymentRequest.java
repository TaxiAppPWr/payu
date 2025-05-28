package com.example.payu.model;

import java.util.List;

public class PaymentRequest {
    private String customerIp="127.0.0.1";
    private String merchantPosId="490009";
    private String description;
    private String currencyCode;
    private String totalAmount;
    private Buyer buyer;

    public void setCustomerIp(String customerIp) {
        this.customerIp = customerIp;
    }

    public void setMerchantPosId(String merchantPosId) {
        this.merchantPosId = merchantPosId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }


    public String getCustomerIp() {
        return customerIp;
    }

    public String getMerchantPosId() {
        return merchantPosId;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public Buyer getBuyer() {
        return buyer;
    }

}
