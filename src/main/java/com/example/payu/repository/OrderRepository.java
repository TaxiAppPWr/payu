package com.example.payu.repository;

import com.example.payu.model.Order;
import com.example.payu.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByStatusIn(List<Status> statuses);

}
