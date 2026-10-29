/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banking.fraud_detection_service.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {
    
    private final FraudDetectionService fraudDetectionService;
    
    
    /**
     * Listens to transaction.initiated topic
     * Every transaction goes through fraud check before completing
     */
    
    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-group")
    public void consumeTransactionInitiated(
            @Payload Map<String, Object> payload){
        
        log.info("Received transaction for fraud check: {}", payload.get("transactionId"));
        
        try{
            fraudDetectionService.checkTransaction(payload);
            
        }catch(Exception e){
            
        }
    }
}
