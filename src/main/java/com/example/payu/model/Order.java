package com.example.payu.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private Integer totalAmount;
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;
    private LocalDateTime createdAt = LocalDateTime.now();
}
