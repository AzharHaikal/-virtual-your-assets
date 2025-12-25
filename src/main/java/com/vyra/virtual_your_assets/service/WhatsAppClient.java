//package com.vyra.virtual_your_assets.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Map;
//
//@Service
//public class WhatsAppClient {
//
//    @Value("${whatsapp.phone-number-id}")
//    private String phoneNumberId;
//
//    @Value("${whatsapp.access-token}")
//    private String accessToken;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public void sendMessage(String phoneNumber, String message) {
//
//        String url = "https://graph.facebook.com/v18.0/"
//                + phoneNumberId + "/messages";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(accessToken);
//
//        Map<String, Object> body = Map.of(
//                "messaging_product", "whatsapp",
//                "to", phoneNumber, // contoh: 6289xxxxxxxxx
//                "type", "text",
//                "text", Map.of(
//                        "body", message
//                )
//        );
//
//        HttpEntity<Map<String, Object>> request =
//                new HttpEntity<>(body, headers);
//
//        ResponseEntity<String> response =
//                restTemplate.postForEntity(url, request, String.class);
//
//        if (!response.getStatusCode().is2xxSuccessful()) {
//            throw new RuntimeException("Failed to send WhatsApp message");
//        }
//    }
//}