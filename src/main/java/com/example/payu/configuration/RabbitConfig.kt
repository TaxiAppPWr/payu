package com.example.payu.configuration
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.rabbit.core.RabbitTemplate

@Configuration
open class RabbitConfig {

    @Bean
    open fun jsonMessageConverter(): MessageConverter {
        val converter = Jackson2JsonMessageConverter()
        converter.setAlwaysConvertToInferredType(true)
        return converter
    }

    @Bean
    open fun rabbitTemplate(connectionFactory: ConnectionFactory, jsonMessageConverter: MessageConverter): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jsonMessageConverter
        return rabbitTemplate
    }
}