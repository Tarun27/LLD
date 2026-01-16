package com.tarun.upi.service;

import com.tarun.upi.exception.InactiveAccountException;
import com.tarun.upi.exception.InsufficientBalanceException;
import com.tarun.upi.model.BankAccount;
import com.tarun.upi.model.Transaction;
import com.tarun.upi.model.User;
import com.tarun.upi.model.enums.AccountStatus;
import com.tarun.upi.model.enums.TransactionStatus;
import com.tarun.upi.psp.PaymentServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    PaymentServiceProvider psp;
    TransactionService txService;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   public PaymentService(PaymentServiceProvider psp, TransactionService txService) {
        this.psp = psp;
        this.txService = txService;
    }


    public Transaction pay(User from, BankAccount src, User to, BankAccount dest, BigDecimal amount) throws InactiveAccountException, InsufficientBalanceException, InterruptedException {

        if (!from.getAccountStatus().isActive() || !to.getAccountStatus().isActive()) throw new InactiveAccountException();
        if (src.getStatus() != AccountStatus.ACTIVE || dest.getStatus() != AccountStatus.ACTIVE)
            throw new InactiveAccountException();
        if (src.getBalance().compareTo(amount) < 0)
            throw new InsufficientBalanceException();

        // lock ordering avoids deadlock
        BankAccount first = src.getAccountNumber().compareTo(dest.getAccountNumber()) < 0 ? src : dest;
        BankAccount second = first == src ? dest : src;

        first.getLock().lock();
        second.getLock().lock();

        Transaction tx;
        try {
            src.setBalance( src.getBalance().subtract(amount) );
            dest.setBalance( dest.getBalance().add(amount));

            tx = new Transaction(UUID.randomUUID().toString(),
                    from.getUserId(), to.getUserId(),
                    src.getAccountNumber(), src.getBankName(), dest.getAccountNumber(), dest.getBankName(),amount,
                    TransactionStatus.PENDING, LocalDateTime.now());

            psp.process(tx);
            txService.record(tx);
            if (tx.getStatus() == TransactionStatus.PENDING)
                retry(tx);

        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            second.getLock().unlock();
            first.getLock().unlock();
        }
        return tx;
    }


    void retry(Transaction tx) throws InterruptedException, TimeoutException {
        final long deadline = System.currentTimeMillis() + 120_000; // 2 minutes from now
        final int maxRetries = 24; // 120 seconds / 5 seconds = 24 attempts

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            // Check if we've exceeded the deadline
            if (System.currentTimeMillis() >= deadline) {
                throw new TimeoutException("Transaction still pending after 120 seconds");
            }

            try {
                TransactionStatus status = psp.getTransactionStatus(tx.getTransactionId());
                tx.setStatus(status);

                // If no longer pending, we're done
                if (status != TransactionStatus.PENDING) {
                    return;
                }

            } catch (Exception e) {
                // Log the error but continue retrying
                log.error("Error fetching transaction status, attempt " + (attempt + 1), e);
            }

            // Wait 5 seconds before next retry (except after last attempt)
            if (attempt < maxRetries - 1) {
                Thread.sleep(5000);
            }
        }

        // If we get here, transaction is still pending after all retries
        throw new TimeoutException("Transaction still pending after maximum retries");
    }

}
