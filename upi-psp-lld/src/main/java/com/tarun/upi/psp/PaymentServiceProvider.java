package com.tarun.upi.psp;

import com.tarun.upi.model.Transaction;
import com.tarun.upi.model.enums.TransactionStatus;

public interface PaymentServiceProvider {

    void process(Transaction transaction);

    public TransactionStatus getTransactionStatus(String transactionId);
}
