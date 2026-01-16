package com.tarun.upi.psp;

import com.tarun.upi.model.Transaction;
import com.tarun.upi.model.enums.TransactionStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockPSP implements PaymentServiceProvider {

    Map<String, Transaction> transactionMap = new ConcurrentHashMap<>();

    @Override
    public TransactionStatus getTransactionStatus(String transactionId) {
        Transaction tx = transactionMap.get(transactionId);
        return tx.getStatus();
    }

    @Override
    public void process(Transaction tx) {
        transactionMap.put(tx.getTransactionId(),tx);
       tx.setStatus(TransactionStatus.SUCCESS);
    }


}
