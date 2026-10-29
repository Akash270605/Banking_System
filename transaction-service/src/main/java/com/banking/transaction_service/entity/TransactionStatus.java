/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.banking.transaction_service.entity;

/**
 *
 * @author Leveno
 */

/**
 * PENDING -> PROCESSING -> COMPLETED (clean Transaction)
 *                                                     -> PENDING_VERIFICATION (suspicious detected)
 *                                                                  -> COMPLETED (verified)
 *                                                                  -> FLAGGED (SAGA REFUND)
 *                                                    -> FAILED
 *                                                    -> FLAGGED
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
