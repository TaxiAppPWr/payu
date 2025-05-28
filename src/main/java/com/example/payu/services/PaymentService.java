package com.example.payu.services;
import com.example.payu.model.*;
import com.example.payu.repository.OrderRepository;

import com.example.payu.components.AccessToken;
import com.example.payu.components.ClientConfig;
import com.example.payu.components.PaymentData;
import com.example.payu.repository.RefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.engine.transaction.jta.platform.internal.SunOneJtaPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PaymentService {

    private final ClientConfig clientConfig;
    private final WebClient.Builder webClientBuilder;
    private final AccessToken accessToken;
    private final PaymentData paymentData;
    private final OrderRepository orderRepository;
    private final AtomicReference<String> latestOrderId = new AtomicReference<>();
    private final AtomicReference<String> latestRefundId = new AtomicReference<>();
    private final RefundRepository refundRepository;

    @Autowired
    public PaymentService(ClientConfig clientConfig, WebClient.Builder webClientBuilder, AccessToken accessToken, PaymentData paymentData, OrderRepository orderRepository, RefundRepository refundRepository) {
        this.clientConfig = clientConfig;
        this.webClientBuilder = webClientBuilder;
        this.accessToken = accessToken;
        this.paymentData =  paymentData;
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
    }



    public Mono<JsonNode> paymentPayU(PaymentRequest paymentRequest) {
        String url = paymentData.getPaymentUrl();

        PaymentRequest orderRequest = new PaymentRequest();
        orderRequest.setDescription(paymentRequest.getDescription());
        orderRequest.setCurrencyCode(paymentRequest.getCurrencyCode());
        orderRequest.setTotalAmount(paymentRequest.getTotalAmount());
        orderRequest.setBuyer(paymentRequest.getBuyer());

        return webClientBuilder.baseUrl(url)
                .build()
                .post()
                .uri("")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    String orderId = response.path("orderId").asText();
                    String status = response.path("status").path("statusCode").asText();

                    Order order = new Order();
                    order.setOrderId(orderId);
                    order.setStatus(status);
                    order.setDescription(paymentRequest.getDescription());
                    order.setCurrencyCode(paymentRequest.getCurrencyCode());
                    order.setTotalAmount(Integer.valueOf(paymentRequest.getTotalAmount()));
                    orderRepository.save(order);

                    latestOrderId.set(orderId);
                    System.out.println("PayU orderId = " + orderId);


                    return response;
                });
    }

    public Flux<JsonNode> getOrderStatus() {
        List<Order> pendingOrders = orderRepository.findAllByStatus("NEW");

        if (pendingOrders.isEmpty()) {
            return Flux.error(new RuntimeException("Brak zamówień ze statusem 'NEW'."));
        }

        return Flux.fromIterable(pendingOrders)
                .flatMap(order -> {
                    String orderId = order.getOrderId();
                    String url = UriComponentsBuilder
                            .fromHttpUrl(paymentData.getPaymentUrl())
                            .pathSegment(orderId)
                            .toUriString();

                    return webClientBuilder.build()
                            .get()
                            .uri(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(response -> {
                                JsonNode ordersNode = response.path("orders");
                                if (ordersNode.isArray() && ordersNode.size() > 0) {
                                    String newStatus = ordersNode.get(0).path("status").asText();
                                    String currentStatus = order.getStatus();
                                    if (!newStatus.equalsIgnoreCase(currentStatus)) {
                                        order.setStatus(newStatus);
                                        orderRepository.save(order);
                                        System.out.println("Zamówienie " + orderId + " zaktualizowane na: " + newStatus);
                                        System.out.println("Płatność udana!");
                                    }
                                }
                                return response;
                            });
                });
    }



    public Mono<JsonNode> getOrderStatusById(String orderId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(paymentData.getPaymentUrl())
                .pathSegment(orderId)
                .toUriString();

        return webClientBuilder.build()
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(JsonNode.class);


    }


    public Mono<JsonNode> refundOrder(RefundRequest refundRequest) {
        String orderId = getLatestOrderId();
        if (orderId == null || orderId.isBlank()) {
            return Mono.error(new RuntimeException("Brak ważnego orderId do refundacji."));
        }

        return getOrderStatusById(orderId)
                .flatMap(statusJson -> {
                    JsonNode orderNode = statusJson.path("orders").get(0);
                    String statusid = orderNode.path("status").asText();

                    if (!statusid.equals("COMPLETED") && !statusid.equals("WAITING_FOR_CONFIRMATION")) {
                        return Mono.error(new RuntimeException("Zamówienie nie zostało opłacone lub nie kwalifikuje się do refundacji. Status: " + statusid));
                    }

                    int totalAmount = orderNode.path("totalAmount").asInt();
                    int refundAmount = Integer.parseInt(refundRequest.getAmount());

                    if (refundAmount > totalAmount) {
                        return Mono.error(new RuntimeException("Kwota refundacji (" + refundAmount + ") przekracza wartość zamówienia (" + totalAmount + ")."));
                    }

                    String url = "https://secure.snd.payu.com/api/v2_1/orders/" + orderId + "/refunds";

                    ObjectNode refundJson = JsonNodeFactory.instance.objectNode();
                    ObjectNode refundDetails = JsonNodeFactory.instance.objectNode();
                    refundDetails.put("description", "Refund");
                    refundDetails.put("amount", refundAmount);
                    refundJson.set("refund", refundDetails);

                    return webClientBuilder.build()
                            .post()
                            .uri(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .bodyValue(refundJson)
                            .retrieve()
                            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                    clientResponse -> clientResponse.bodyToMono(String.class)
                                            .map(body -> new RuntimeException("API error: " + body)))
                            .bodyToMono(JsonNode.class)
                            .map(response -> {
                                System.out.println("Odpowiedź z PayU: " + response.toPrettyString());

                                String refundId = response.path("refund").path("refundId").asText();
                                String refundStatus = response.path("refund").path("status").asText();
                                int amount = Integer.parseInt(response.path("refund").path("amount").asText());
                                String description = response.path("refund").path("description").asText();
                                String currencyCode = response.path("refund").path("currencyCode").asText();

                                Refund refund = new Refund();
                                refund.setOrderId(orderId);
                                refund.setRefundId(refundId);
                                refund.setStatus(refundStatus);
                                refund.setAmount(amount);
                                refund.setDescription(description);
                                refund.setCurrencyCode(currencyCode);

                                refundRepository.save(refund);
                                latestRefundId.set(refundId);

                                System.out.println("Refund zapisany. refundId = " + refund.getDescription());

                                return response;
                            })
                            .doOnError(e -> System.out.println("Błąd podczas refundacji: " + e.getMessage()));
                });
    }





    public Flux<JsonNode> getRefundStatus() {
        List<Refund> pendingRefunds = refundRepository.findAllByStatus("PENDING");

        if (pendingRefunds.isEmpty()) {
            return Flux.error(new RuntimeException("Brak refundacji ze statusem 'PENDING'."));
        }

        return Flux.fromIterable(pendingRefunds)
                .flatMap(refund -> {
                    String orderId = refund.getOrderId();
                    String refundId = refund.getRefundId();

                    String url = "https://secure.snd.payu.com/api/v2_1/orders/" + orderId + "/refunds/" + refundId;

                    return webClientBuilder.build()
                            .get()
                            .uri(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .doOnError(e -> System.out.println("Błąd przy pobieraniu statusu refundacji: " + refundId + " - " + e.getMessage()))
                            .map(response -> {
                                String newStatus = response.path("status").asText();
                                String currentStatus = refund.getStatus();
                                if (!newStatus.equalsIgnoreCase(currentStatus)) {
                                    refund.setStatus(newStatus);
                                    refundRepository.save(refund);
                                    System.out.println("Status refundacji " + refundId + " zaktualizowany na: " + newStatus);
                                    System.out.println("Zwrot wykonany!");
                                }
                                return response;
                            });
                });
    }




    public String getLatestOrderId() {
        return latestOrderId.get();
    }

    public String getLatestRefundId() {
        return latestRefundId.get();
    }
}