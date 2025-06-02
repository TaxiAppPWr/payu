package com.example.payu.dto

data class PaymentStatusUpdatedEvent(
    val paymentStatusUpdatedEventId: Long,
    val paymentId: String,
    val status: String
)