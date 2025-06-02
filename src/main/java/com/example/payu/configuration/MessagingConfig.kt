package com.example.payu.configuration

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MessagingConfig {
    @Value("\${rabbit.exchange.ride.name}")
    private val ride: String? = null

    @Bean
    open fun exchange(): TopicExchange {
        return TopicExchange("$ride")
    }
}