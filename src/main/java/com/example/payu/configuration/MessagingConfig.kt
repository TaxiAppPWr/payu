package com.example.payu.configuration

import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MessagingConfig {
    @Value("\${rabbit.exchange.payment.name}")
    private val paymentExchange: String? = null

    @Bean
    open fun exchange(): TopicExchange {
        return TopicExchange("$paymentExchange")
    }
}