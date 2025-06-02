package com.example.payu.controllers;
import com.example.payu.dto.PaymentRequestTO;
import com.example.payu.model.*;
import com.example.payu.model.PaymentRequest;
import com.example.payu.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payu")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public Mono<ResponseEntity<PaymentResponse>> payment(@RequestBody PaymentRequestTO paymentRequest) {
        return paymentService.paymentPayU(paymentRequest)
                .map(response -> ResponseEntity.ok(new PaymentResponse("success", response)))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body(new PaymentResponse("error", e.getMessage())))
                );
    }

    @PostMapping("/refund")
    public Mono<ResponseEntity<RefundResponseWrapper>> refundOrder(@RequestBody RefundRequest refundRequest) {
        return paymentService.refundOrder(refundRequest)
                .map(response -> ResponseEntity.ok(new RefundResponseWrapper("success", response)))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(500).body(new RefundResponseWrapper("error", e.getMessage())))
                );
    }
}
