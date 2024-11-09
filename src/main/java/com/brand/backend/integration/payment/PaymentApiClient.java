package com.brand.backend.integration.payment;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

@Component
public class PaymentApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> sendPostRequest(String url, HttpHeaders headers, String payload) {
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public ResponseEntity<String> sendGetRequest(String url, HttpHeaders headers) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }
}
