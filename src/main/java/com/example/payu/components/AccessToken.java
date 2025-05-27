package com.example.payu.components;


import org.springframework.stereotype.Component;

@Component
public class AccessToken {
    private String accessToken;

    // Getter
    public String getToken() {
        return accessToken;
    }

    // Setter
    public void setToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
