package com.example.payu.repository;

import com.example.payu.model.Order;
import com.example.payu.model.Refund;
import com.example.payu.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundId(String refundId);
    List<Refund> findAllByStatus(Status status);

}
