package com.tarun.upi.service;

import com.tarun.upi.exception.UPIException;
import com.tarun.upi.model.Bank;
import com.tarun.upi.model.BankAccount;
import com.tarun.upi.model.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BankAccountService {

    public Map<String, BankAccount> userByBankAccount = new HashMap<>();
    ; // key = bankId|accountNo

    public BankAccount addBankAccount(String bankaccId, Bank bank, BigDecimal amount, User user) throws UPIException {
        BankAccount bankAccount = new BankAccount(bankaccId, bank, amount, user.getUserId());
        String key = bank.getBanknName() + "|" + bankaccId;
        if(userByBankAccount.containsKey(key)) throw new UPIException("Account Already exists");
        userByBankAccount.put(key, bankAccount);
        linkBankAccount(user,bankAccount);
        return bankAccount;
    }

    public void linkBankAccount(User user, BankAccount newAccount) {

        String key = newAccount.getBankName() + "|" + newAccount.getAccountNumber();

        // FIRST account → must be primary
        if (user.getLinkedAccounts().isEmpty()) {
            newAccount.isPrimary = true;
            user.setPrimaryAccountKey(key);
        }

        userByBankAccount.put(key, newAccount);
    }

    private void demoteExistingPrimary(User user) {
        String oldKey = user.getPrimaryAccountKey();
        if (oldKey != null) {
            BankAccount oldPrimary = userByBankAccount.get(oldKey);
            oldPrimary.isPrimary = false;
        }
    }

    public void makePrimary(User user, BankAccount account){
        demoteExistingPrimary(user);
        account.isPrimary = true;
        String key = account.getBankName() + "|" + account.getAccountNumber();
        user.setPrimaryAccountKey(key);
    }


}
