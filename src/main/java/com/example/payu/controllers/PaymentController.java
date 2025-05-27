package com.example.payu.controllers;
import com.example.payu.model.*;
import com.example.payu.model.PaymentRequest;
import com.example.payu.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/payu")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }



    @PostMapping("/payment")
    public Mono<ResponseEntity<Map<String, Object>>> payment(@RequestBody PaymentRequest paymentRequest) {
        return paymentService.paymentPayU2(paymentRequest)
                .map(response -> ResponseEntity.ok().body(
                        Map.of("status", "success", "response", response)
                ))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body(
                                Map.of("status", "error", "message", e.getMessage())
                        ))
                );
    }

    @PostMapping("/payment2")
    public ResponseEntity<?> payment2(@RequestBody PaymentRequest paymentRequest) {

            return ResponseEntity.ok(
                    Map.of("status", "success", "message", "")
            );

    }

    @GetMapping("/order/status")
    public Mono<ResponseEntity<Map<String, Object>>> getOrderStatus() {

        return paymentService.getOrderStatus()
                .map(response -> ResponseEntity.ok(
                        Map.of("status", "success", "orderStatus", response)
                ))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(
                                Map.of("status", "error", "message", e.getMessage())
                        )
                ));
    }

    @PostMapping("/refund")
    public Mono<ResponseEntity<Map<String, Object>>> refundOrder(@RequestBody RefundRequest refundRequest
    ) {
        return paymentService.refundOrder(refundRequest)
                .map(response -> ResponseEntity.ok(
                        Map.of("status", "success", "refundResponse", response)
                ))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(
                                Map.of("status", "error", "message", e.getMessage())
                        )
                ));
    }


    @GetMapping("/refund/status")
    public Mono<ResponseEntity<Map<String, Object>>> getRefundStatus() {

                return paymentService.getRefundStatus()
                .map(response -> ResponseEntity.ok(
                        Map.of("status", "success", "orderStatus", response)
                ))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(
                                Map.of("status", "error", "message", e.getMessage())
                        )
                ));
    }

}
