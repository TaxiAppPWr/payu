package com.example.payu.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queues.payment-status}")
    private String paymentStatusQueue;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.payment-status}")
    private String paymentStatusRoutingKey;

    @Bean
    public Queue paymentStatusQueue() {
        return new Queue(paymentStatusQueue, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder.bind(paymentStatusQueue()).to(exchange()).with(paymentStatusRoutingKey);
    }
}