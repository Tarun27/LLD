package com.tarun.upi.model;

import com.tarun.upi.model.enums.EntryType;
import com.tarun.upi.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserTransaction {

    // ----- identity -----
    private final String userId;           // owner of this ledger entry
    private final String transactionId;    // links to Transaction

    // ----- money -----
    private final BigDecimal amount;
    private final EntryType entryType;      // DEBIT or CREDIT
    private TransactionStatus status;       // PENDING / SUCCESS / FAILED

    // ----- bank snapshot (for THIS user) -----
    private final String bankId;            // or bankName
    private final String accountNumber;

    // ----- counterparty info -----
    private final String counterPartyUserId;

    // ----- metadata -----
    private final LocalDateTime createdAt;

    public UserTransaction(String userId,
                           String transactionId,
                           BigDecimal amount,
                           EntryType entryType,
                           TransactionStatus status,
                           String bankId,
                           String accountNumber,
                           String counterPartyUserId,
                           LocalDateTime createdAt) {
        this.userId = userId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.entryType = entryType;
        this.status = status;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.counterPartyUserId = counterPartyUserId;
        this.createdAt = createdAt;
    }

    public void updateStatus(TransactionStatus newStatus) {
        this.status = newStatus;
    }


    // getters only (immutability except status)
}

