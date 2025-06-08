package com.example.payu.components;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class ClientConfig {
    private String id;
    private String secret;
    private String baseUrl;

    public ClientConfig(@Value("${client.id}") String id,
                        @Value("${client.secret}") String secret,
                        @Value("${client.baseUrl}") String baseUrl) {
        this.id = id;
        this.secret = secret;
        this.baseUrl = baseUrl;
    }
}