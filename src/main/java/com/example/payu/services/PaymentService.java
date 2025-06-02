package com.example.payu.services;
import com.example.payu.configuration.PayuProperties;
import com.example.payu.dto.PaymentRequestTO;
import com.example.payu.dto.PaymentStatusUpdatedEvent;
import com.example.payu.model.*;
import com.example.payu.repository.OrderRepository;

import com.example.payu.components.AccessToken;
import com.example.payu.components.ClientConfig;
import com.example.payu.components.PaymentData;
import com.example.payu.repository.RefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PaymentService {
    @Value("${rabbit.topic.payment.status-updated}")
    private String paymentStatusUpdatedTopic;

    private final ClientConfig clientConfig;
    private final WebClient.Builder webClientBuilder;
    private final AccessToken accessToken;
    private final PaymentData paymentData;
    private final OrderRepository orderRepository;
    private final AtomicReference<String> latestOrderId = new AtomicReference<>();
    private final AtomicReference<String> latestRefundId = new AtomicReference<>();
    private final RefundRepository refundRepository;
    private final PayuProperties payuProperties;
    private final TopicExchange exchange;
    private final RabbitTemplate template;

    @Autowired
    public PaymentService(ClientConfig clientConfig, WebClient.Builder webClientBuilder, AccessToken accessToken, PaymentData paymentData, OrderRepository orderRepository, RefundRepository refundRepository, PayuProperties payuProperties, TopicExchange exchange, RabbitTemplate template) {
        this.clientConfig = clientConfig;
        this.webClientBuilder = webClientBuilder;
        this.accessToken = accessToken;
        this.paymentData =  paymentData;
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
        this.payuProperties = payuProperties;
        this.exchange = exchange;
        this.template = template;
    }



    public Mono<JsonNode> paymentPayU(PaymentRequestTO paymentRequest) {
        String url = paymentData.getPaymentUrl();

        PaymentRequest orderRequest = new PaymentRequest();
        orderRequest.setDescription(payuProperties.getPaymentDescription());
        orderRequest.setCurrencyCode(payuProperties.getCurrencyCode());
        orderRequest.setTotalAmount(paymentRequest.getTotalAmount());
        orderRequest.setBuyer(paymentRequest.getBuyer());
        orderRequest.setCustomerIp(payuProperties.getCustomerIp());
        orderRequest.setMerchantPosId(payuProperties.getMerchantPosId());
        orderRequest.setContinueUrl(payuProperties.getContinueUrl());

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
                    order.setStatus(Status.valueOf(status));
                    order.setTotalAmount(Integer.valueOf(paymentRequest.getTotalAmount()));
                    orderRepository.save(order);

                    latestOrderId.set(orderId);
                    System.out.println("PayU orderId = " + orderId);


                    return response;
                });
    }

    public Flux<JsonNode> getOrderStatus() {
        List<Order> ordersToCheck = orderRepository.findAllByStatusIn(List.of(Status.NEW,Status.SUCCESS,Status.PENDING));


        if (ordersToCheck.isEmpty()) {
//            return Flux.error(new RuntimeException("Brak zamówień ze statusem 'NEW' lub 'SUCCESS'."));
            return Flux.empty();
        }


        return Flux.fromIterable(ordersToCheck)
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
                            .flatMap(response -> {
                                JsonNode ordersNode = response.path("orders");
                                if (ordersNode.isArray() && ordersNode.size() > 0) {
                                    String newStatus = ordersNode.get(0).path("status").asText();
                                    Status currentStatus = order.getStatus();
                                    Status parsedStatus = Status.valueOf(newStatus.toUpperCase());

                                    LocalDateTime orderExpiredTime = LocalDateTime.ofInstant(
                                            Instant.now().minus(Duration.ofMinutes(5)),
                                            ZoneId.systemDefault()
                                    );

                                    if (order.getCreatedAt().isBefore(orderExpiredTime)) {
                                        parsedStatus = Status.CANCELED;
                                    }

                                    if (!parsedStatus.equals(currentStatus)) {
                                        order.setStatus(parsedStatus);
                                        Status finalParsedStatus = parsedStatus;
                                        return Mono.fromCallable(() -> {
                                            orderRepository.save(order);
                                            System.out.println("Zamówienie " + orderId + " zaktualizowane na: " + finalParsedStatus);
                                            if (finalParsedStatus == Status.COMPLETED) {
                                                System.out.println("Płatność zakończona pomyślnie!");
                                            }

                                            PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(1, order.getOrderId(), order.getStatus().name());
                                            template.convertAndSend(exchange.getName(), paymentStatusUpdatedTopic, event);
                                            return response;
                                        });
                                    }
                                }
                                return Mono.just(response);
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
        String orderId = refundRequest.getOrderId();
//        if (orderId == null || orderId.isBlank()) {
//            return Mono.error(new RuntimeException("Brak ważnego orderId do refundacji."));
//        }

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
                    refundDetails.put("description", payuProperties.getRefundDescription());
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

                                Refund refund = new Refund();
                                refund.setOrderId(orderId);
                                refund.setRefundId(refundId);
                                refund.setStatus(Status.valueOf(refundStatus));
                                refund.setAmount(amount);

                                refundRepository.save(refund);
                                latestRefundId.set(refundId);

                                System.out.println("Refund zapisany. refundId = " + refund.getRefundId());

                                return response;
                            })
                            .doOnError(e -> System.out.println("Błąd podczas refundacji: " + e.getMessage()));
                });
    }





    public Flux<JsonNode> getRefundStatus() {
        List<Refund> pendingRefunds = refundRepository.findAllByStatus(Status.PENDING);

        if (pendingRefunds.isEmpty()) {
//            return Flux.error(new RuntimeException("Brak refundacji ze statusem 'PENDING'."));
            return Flux.empty();
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
                                Status currentStatus = refund.getStatus();
                                if (!newStatus.equalsIgnoreCase(String.valueOf(currentStatus))) {
                                    refund.setStatus(Status.valueOf(newStatus));
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

}