package com.tarun.upi.model;

import com.tarun.upi.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    String accountNumber;
    String bankName;
    String userId;
    BigDecimal balance;
    AccountStatus status;
    /**
     * The lock is part of the BankAccount because concurrency control is required at the account level, not at the service or application level.
     * Each bank account represents an independent mutable resource (its balance), so it should own its own lock.
     */
    ReentrantLock lock; // for concurrency
    public boolean isPrimary;
    public String primaryAccountKey; // bankId|accountNo

    public BankAccount(String a1, Bank bank, BigDecimal bigDecimal, String userId) {
        this.accountNumber = a1;
        this.bankName = bank.banknName; // Note: Bank class uses 'banknName'
        this.userId = userId; // Not provided in constructor
        this.balance = bigDecimal;
        this.status = AccountStatus.ACTIVE; // Default to ACTIVE
        this.lock = new ReentrantLock();
    }
}