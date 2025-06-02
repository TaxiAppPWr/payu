package com.example.payu.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@Setter
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    @Column(name = "refund_id")
    private String refundId;

    @Enumerated(EnumType.STRING)
    private Status status;
    private Integer amount;
    private LocalDateTime createdAt = LocalDateTime.now();
}
