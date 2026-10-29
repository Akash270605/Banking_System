/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banking.transaction_service.Repository;

import com.banking.transaction_service.entity.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Leveno
 */
public interface TransactionRepository extends JpaRepository<Transaction, String>{
    List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
