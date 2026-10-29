/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banking.transaction_service.client;

import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Leveno
 */

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountServiceClient {
    
    @PutMapping("/api/v1/accounts/{accountNumber}/deduct")
    String deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount);
    
    @PutMapping("/api/v1/accounts/{accountNumber}/credit")
    String creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount);
}
