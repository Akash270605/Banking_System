/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.banking.payment_service.repository;

import com.banking.payment_service.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Leveno
 */
public interface PaymentRepository extends JpaRepository<Payment, String>{
    
    Optional<Payment> findByRazorpayOrderId(String orderId);
}
