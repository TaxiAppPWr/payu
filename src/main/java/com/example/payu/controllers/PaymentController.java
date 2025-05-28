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
        return paymentService.paymentPayU(paymentRequest)
                .map(response -> ResponseEntity.ok().body(
                        Map.of("status", "success", "response", response)       //url status
                ))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body(
                                Map.of("status", "error", "message", e.getMessage())
                        ))
                );
    }

    @PostMapping("/refund")
    public Mono<ResponseEntity<Map<String, Object>>> refundOrder(@RequestBody RefundRequest refundRequest
    ) {
        return paymentService.refundOrder(refundRequest)
                .map(response -> ResponseEntity.ok(
                        Map.of("status", "success", "refundResponse", response)     //status
                ))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(
                                Map.of("status", "error", "message", e.getMessage())
                        )
                ));
    }


}
