package com.tarun.upi.service;

import com.tarun.upi.model.Transaction;
import com.tarun.upi.model.UserTransaction;
import com.tarun.upi.model.enums.EntryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionService {

    private final Map<String, List<UserTransaction>> userLedger = new ConcurrentHashMap<>();

    private final Map<String, List<Transaction>> transactionsByAccount = new ConcurrentHashMap<>();


    public void record(Transaction tx) {

        // ---------- account-level ----------
        transactionsByAccount.computeIfAbsent(tx.getFromAccountNo(), k -> new ArrayList<>()).add(tx);

        transactionsByAccount.computeIfAbsent(tx.getToAccountNo(), k -> new ArrayList<>()).add(tx);

        // ---------- user-level ledger ----------
        addUserEntry(tx.getFromUserId(), tx, EntryType.DEBIT, tx.getToUserId(), tx.getFromBankId(), tx.getFromAccountNo());

        addUserEntry(tx.getToUserId(), tx, EntryType.CREDIT, tx.getFromUserId(), tx.getToBankId(), tx.getToAccountNo());

    }

    private void addUserEntry(String userId, Transaction tx, EntryType type, String counterPartyUserId, String userBankId, String userAccNo) {

        UserTransaction entry = new UserTransaction(userId, tx.getTransactionId(), tx.getAmount(), type, tx.getStatus(),
                userBankId, userAccNo,counterPartyUserId,tx.getCreatedAt());

        userLedger.computeIfAbsent(userId, k -> new ArrayList<>()).add(entry);
    }

    public List<UserTransaction> getUserLedger(String userId) {
        return userLedger.getOrDefault(userId, List.of());
    }

    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        return transactionsByAccount.getOrDefault(accountNumber, List.of());
    }

}
