package com.example.payu.components;

import com.example.payu.services.PaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusScheduler {

    private final PaymentService paymentService;

    public PaymentStatusScheduler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 10000)
    public void checkOrderStatus() {
        System.out.println("Sprawdzam status płatności...");
        paymentService.getOrderStatus()
                .doOnNext(status -> System.out.println("Status płatności sprawdzony.\n" + status))
                .doOnError(error -> System.out.println("Błąd przy sprawdzaniu statusu: " + error.getMessage()))
                .subscribe();
    }

    @Scheduled(fixedDelay = 10000)
    public void checkRefundStatus() {
        System.out.println("Sprawdzam status refundacji...");
        paymentService.getRefundStatus()
                .doOnNext(status -> System.out.println("Status refundacji sprawdzony."))
                .doOnError(error -> System.out.println("Błąd przy sprawdzaniu refundacji: " + error.getMessage()))
                .subscribe();
    }
}
