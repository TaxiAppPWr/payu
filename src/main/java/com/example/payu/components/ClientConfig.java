package com.example.payu.components;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class ClientConfig {
    private String id = "490009";
    private String secret = "9a3f26c3823f06e7092ec96a8ffd98a5";
    private String baseUrl = "https://secure.snd.payu.com/pl/standard/user/oauth/authorize";

    public ClientConfig() {}

    // Gettery i settery
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}