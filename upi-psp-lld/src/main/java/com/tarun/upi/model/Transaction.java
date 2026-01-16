package com.tarun.upi.model;

import com.tarun.upi.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    String transactionId;
    String fromUserId;
    String toUserId;

    String fromAccountNo;
    String fromBankId;

    String toAccountNo;
    String toBankId;

    BigDecimal amount;
    TransactionStatus status;
    LocalDateTime createdAt;

}
