package com.tarun;

import com.tarun.upi.exception.BankServerDownException;
import com.tarun.upi.exception.InactiveAccountException;
import com.tarun.upi.exception.InsufficientBalanceException;
import com.tarun.upi.exception.UPIException;
import com.tarun.upi.model.BankAccount;
import com.tarun.upi.model.Transaction;
import com.tarun.upi.model.User;
import com.tarun.upi.model.enums.EntryType;
import com.tarun.upi.model.enums.TransactionStatus;
import com.tarun.upi.psp.MockPSP;
import com.tarun.upi.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws UPIException, BankServerDownException, InactiveAccountException, InsufficientBalanceException, InterruptedException {

        BankService bankService = new BankService();
        bankService.addBank("HDFC", true);
        bankService.addBank("ICICI", true);
        bankService.addBank("IDFC", true);

        UserService userService = new UserService();

        User u1 = userService.createUser("U1", "Alice", "999");
        User u2 = userService.createUser("U2", "Bob", "888");

        BankAccountService bankAccountService = new BankAccountService();

        BankAccount a1 = bankAccountService.addBankAccount("A1", bankService.getBank("HDFC"), new BigDecimal(1000), u1);
        BankAccount a2 = bankAccountService.addBankAccount("A2", bankService.getBank("ICICI"), new BigDecimal(500), u2);
        BankAccount a3 = bankAccountService.addBankAccount("A3", bankService.getBank("IDFC"), new BigDecimal(1000), u1);


        u1.getLinkedAccounts().put(a1.getAccountNumber(), a1);
        u1.getLinkedAccounts().put(a3.getAccountNumber(), a3);
        u2.getLinkedAccounts().put(a2.getAccountNumber(), a2);


        TransactionService txService = new TransactionService();
        PaymentService paymentService = new PaymentService(new MockPSP(), txService);

        System.out.println("Alice HDFC balance before transaction: " + u1.getLinkedAccounts().get(a1.getAccountNumber()).getBalance().toString());
        System.out.println("Bob ICICI balance before transaction: " + u2.getLinkedAccounts().get(a2.getAccountNumber()).getBalance().toString() + "\n");

        System.out.println("---- Payment Start ----");
        System.out.println("Alice pays 200 to Bob");

        Transaction tx = paymentService.pay(u1, a1, u2, a2, new BigDecimal("200"));

        System.out.println("TX ID: " + tx.getTransactionId());
        System.out.println("Status: " + tx.getStatus());
        System.out.println("Alice Balance: " + a1.getBalance());
        System.out.println("Bob Balance: " + a2.getBalance());

        Map<String, User> userMap = userService.usersById;

        System.out.println("\n---- Alice Transactions ----");
        txService.getUserLedger("U1").forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));


        System.out.println("---- Payment Start ----");
        System.out.println("Bob pays 50 to Alice HDFC Account");

        //  A user should be able to make a payment to another user by providing username,
        //  phone number or bank account.

        // alice phone no = 999, id = U1, acc no-> A1

        User alice1 = userService.usersById.get("U1");
        User alice2 = userService.usersByPhone.get("999");

        // bank account => Bank + acc No -> HDFC,A1
        String key = "HDFC" + "|" + "A1";
        User alice3 = userService.usersById.get(bankAccountService.userByBankAccount.get(key).getUserId());

        Transaction tx2 = paymentService.pay(u2, a2, alice3, a1, new BigDecimal("50"));


        System.out.println("TX ID: " + tx2.getTransactionId());
        System.out.println("Status: " + tx2.getStatus());
        System.out.println("Alice Balance: " + a1.getBalance());
        System.out.println("Bob Balance: " + a2.getBalance());


        System.out.println("---- Payment Start ----");
        System.out.println("Bob pays 50 to Alice IDFC account");

        Transaction tx3 = paymentService.pay(u2, a2, u1, a3, new BigDecimal("50"));

        System.out.println("TX ID: " + tx3.getTransactionId());
        System.out.println("Status: " + tx3.getStatus());
        System.out.println("Alice Balance: " + a3.getBalance());
        System.out.println("Bob Balance: " + a2.getBalance());


        System.out.println("\n---- Alice HDFC Transactions ----");

        txService.getUserLedger("U1").stream().filter(b -> b.getBankId().equals("HDFC")).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));


        System.out.println("\n---- Alice IDFC Transactions ----");

        txService.getUserLedger("U1").stream().filter(b -> b.getBankId().equals("IDFC")).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));

        System.out.println("\n---- Bob Transactions ----");
        txService.getUserLedger("U2").forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));

// user should be able to search transactions based on other payee/payer users.

        System.out.println("\n---- Transactions of Alice with Bob ----");

        txService.getUserLedger("U1").stream().filter(ut -> ut.getCounterPartyUserId().equals("U2")).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));

        System.out.println("\n---- Transactions of Bob with Alice ----");

        txService.getUserLedger("U2").stream().filter(ut -> ut.getCounterPartyUserId().equals("U1")).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));


        System.out.println("\n---- Transactions when Alice is Payer ----");

        txService.getUserLedger("U1").stream().filter(ut -> ut.getEntryType().equals(EntryType.DEBIT)).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));

        System.out.println("\n---- Transactions when Alice is Payee ----");

        txService.getUserLedger("U1").stream().filter(ut -> ut.getEntryType().equals(EntryType.CREDIT)).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));


        // handle concurrent transactions

        // ----- Two concurrent payments -----
        Runnable task1 = () -> {
            try {
                Transaction tx4 = paymentService.pay(u2, a2, u1, a3, new BigDecimal("80"));

                System.out.println("Thread-1: Payment SUCCESS");
            } catch (Exception | InactiveAccountException | InsufficientBalanceException e) {
                System.out.println("Thread-1: " + e.getMessage());
            }
        };

        Runnable task2 = () -> {
            try {
                Transaction tx4 = paymentService.pay(u1, a1, u2, a2, new BigDecimal("150"));

                System.out.println("Thread-2: Payment SUCCESS");
            } catch (Exception | InactiveAccountException | InsufficientBalanceException e) {
                System.out.println("Thread-2: " + e.getMessage());
            }

        };

        Thread t1 = new Thread(task1, "TXN-THREAD-1");
        Thread t2 = new Thread(task2, "TXN-THREAD-2");

        // ----- Start simultaneously -----
        t1.start();
        t2.start();

        // ----- Wait for completion -----
        t1.join();
        t2.join();

        System.out.println("\n---- Alice IDFC Transactions ----");

        txService.getUserLedger("U1").stream().filter(b -> b.getBankId().equals("IDFC")).forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));

        System.out.println("\n---- Bob Transactions ----");
        txService.getUserLedger("U2").forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));

        System.out.println("\nAlice Balance: " + a1.getBalance());
        System.out.println("Bob Balance: " + a2.getBalance());


        // Search transactions based on date range and transaction status

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        // Assuming we want to filter Alice's transactions in the last 1 day
        System.out.println("\n---- Alice Transactions in the last 1 day ----");

         txService.getUserLedger("U1")
                .stream()
                .filter(t -> from == null || !t.getCreatedAt().isBefore(from))
                .filter(t -> to == null || !t.getCreatedAt().isAfter(to))
                .forEach(t -> System.out.println(userMap.get(t.getUserId()).getName() + " " + t.getAmount() + " " + t.getEntryType() + "  " + t.getStatus() + " with User " + userMap.get(t.getCounterPartyUserId()).getName()));
        // for search based on transaction status, similar filtering can be applied
        Transaction tx5 = new Transaction();
        try{
             tx5 = paymentService.pay(u2, a2, u1, a3, new BigDecimal("1000"));
        }catch (InsufficientBalanceException e){
            System.out.println("\n---- Transaction with Insufficient Balance ----");
            System.out.println("TX ID: " + tx5.getTransactionId());
            System.out.println("Status: " + TransactionStatus.FAILED);
        }

        System.out.println("\nAlice HDFC Balance: " + a1.getBalance());
        System.out.println("\nAlice IDFC Balance: " + a3.getBalance());
        System.out.println("Bob Balance: " + a2.getBalance());


    }
}