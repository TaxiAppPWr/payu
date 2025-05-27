package com.example.payu.services;

import com.example.payu.components.AccessToken;
import com.example.payu.components.ClientConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AccessTokenService {

    private final ClientConfig clientConfig;
    private final WebClient.Builder webClientBuilder;
    private final AccessToken accessToken;

    @Autowired
    public AccessTokenService(ClientConfig clientConfig, WebClient.Builder webClientBuilder,AccessToken accessToken) {
        this.clientConfig = clientConfig;
        this.webClientBuilder = webClientBuilder;
        this.accessToken = accessToken;
    }


    @PostConstruct
    public void init() {
        getTokenFromPayu();
        System.out.println("token "+accessToken.getToken());
    }

    public void getTokenFromPayu() {


        String url = clientConfig.getBaseUrl();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientConfig.getId());
        formData.add("client_secret", clientConfig.getSecret());
        formData.add("grant_type", "client_credentials");

        String response = webClientBuilder.baseUrl(url)
                .build()
                .post()
                .uri(UriComponentsBuilder.fromUriString(url).build().toUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        String token = parseKeyFromResponse(response);
        accessToken.setToken(token);
    }

    private String parseKeyFromResponse(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.path("access_token").asText();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}