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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
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


    public void paymentPayU(PaymentRequest paymentRequest) {


        String url = paymentData.getPaymentUrl();

        Buyer buyer = new Buyer();
        buyer.setEmail(paymentRequest.getBuyer().getEmail());
        buyer.setPhone(paymentRequest.getBuyer().getPhone());
        buyer.setFirstName(paymentRequest.getBuyer().getFirstName());
        buyer.setLastName(paymentRequest.getBuyer().getLastName());
        buyer.setLanguage(paymentRequest.getBuyer().getLanguage());

        Product product = new Product();
        product.setName(paymentRequest.getProducts().get(0).getName());
        product.setUnitPrice(paymentRequest.getProducts().get(0).getUnitPrice());
        product.setQuantity(paymentRequest.getProducts().get(0).getQuantity());
        PaymentRequest orderRequest = new PaymentRequest();
        orderRequest.setNotifyUrl(paymentRequest.getNotifyUrl());
        orderRequest.setCustomerIp(paymentRequest.getCustomerIp());
        orderRequest.setMerchantPosId(paymentRequest.getMerchantPosId());
        orderRequest.setDescription(paymentRequest.getDescription());
        orderRequest.setCurrencyCode(paymentRequest.getCurrencyCode());
        orderRequest.setTotalAmount(paymentRequest.getTotalAmount());
        orderRequest.setContinueUrl(paymentRequest.getContinueUrl());
        orderRequest.setBuyer(paymentRequest.getBuyer());
        orderRequest.setProducts(paymentRequest.getProducts());

        String response = webClientBuilder.baseUrl(url)
                .build()
                .post()
                .uri("")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        String redirectUrl = parseRedirectFromResponse(response);
        System.out.println("Klient powinien być przekierowany do: " + redirectUrl);
    }

    public Mono<JsonNode> paymentPayU2(PaymentRequest paymentRequest) {
        String url = paymentData.getPaymentUrl();

        PaymentRequest orderRequest = new PaymentRequest();
        orderRequest.setNotifyUrl(paymentRequest.getNotifyUrl());
        orderRequest.setCustomerIp(paymentRequest.getCustomerIp());
        orderRequest.setMerchantPosId(paymentRequest.getMerchantPosId());
        orderRequest.setDescription(paymentRequest.getDescription());
        orderRequest.setCurrencyCode(paymentRequest.getCurrencyCode());
        orderRequest.setTotalAmount(paymentRequest.getTotalAmount());
        orderRequest.setContinueUrl(paymentRequest.getContinueUrl());
        orderRequest.setBuyer(paymentRequest.getBuyer());
        orderRequest.setProducts(paymentRequest.getProducts());

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

    public Mono<JsonNode> getOrderStatus() {

        String orderId = getLatestOrderId();

        if (orderId == null) {
            return Mono.error(new RuntimeException("Brak orderId lub refundId do sprawdzenia statusu refundacji."));
        }
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
                    JsonNode orders = response.path("orders");
                    if (orders.isArray() && orders.size() > 0) {
                        String newStatus = orders.get(0).path("status").asText();

                        orderRepository.findByOrderId(orderId).ifPresent(order -> {
                            order.setStatus(newStatus);
                            orderRepository.save(order);
                            System.out.println("Status zamówienia " + orderId + " zaktualizowany na: " + newStatus);
                        });
                    }

                    return response;
                });
    }


    public String parseRedirectFromResponse(String responseJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseJson);
            return root.path("redirectUri").asText();
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się sparsować redirectUri z odpowiedzi PayU", e);
        }
    }

    public Mono<JsonNode> refundOrder(RefundRequest refundRequest) {
        String orderId = getLatestOrderId();
        if (orderId == null || orderId.isBlank()) {
            return Mono.error(new RuntimeException("Brak ważnego orderId do refundacji."));
        }

        return getOrderStatus()
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





    public Mono<JsonNode> getRefundStatus() {
        String orderId = getLatestOrderId();
        String refundId = getLatestRefundId();
        System.out.println("latestOrderId ustawione na: " + orderId);
        System.out.println("latestRefundId ustawione na: " + refundId);


        if (orderId == null || refundId == null) {
            return Mono.error(new RuntimeException("Brak orderId lub refundId do sprawdzenia statusu refundacji."));
        }

        String url = "https://secure.snd.payu.com/api/v2_1/orders/" + orderId + "/refunds/" + refundId;

        return webClientBuilder.build()
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> System.out.println("Błąd podczas pobierania statusu refundacji: " + e.getMessage()))
                .map(response -> {
                        String newStatus = response.path("status").asText();
                        System.out.println(newStatus);
                        refundRepository.findByRefundId(refundId).ifPresent(refund -> {
                            refund.setStatus(newStatus);
                            refundRepository.save(refund);
                            System.out.println("Status zamówienia " + refundId + " zaktualizowany na: " + newStatus);
                        });

                    return response;
                });
    }



    public String getLatestOrderId() {
        return latestOrderId.get();
    }

    public String getLatestRefundId() {
        return latestRefundId.get();
    }
}