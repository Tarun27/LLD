package com.tarun.upi.model;

import com.tarun.upi.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple User model for UPI system.
 * Contains name, phone number and active status (true = active, false = deactivated).
 * One phone → one user ✅
 *
 * Multiple bank accounts allowed ✅
 *
 * One mandatory primary bank account ✅
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private String userId;
    private String name;
    private String phoneNumber;
    // status represents whether the user is active or deactivated
    private AccountStatus accountStatus;

    Map<String, BankAccount> linkedAccounts; // key = accountNumber
    String primaryAccountKey;

    public User(String userId, String name, String phoneNumber) {
        this.userId = userId;
        this.name = name;
        this. phoneNumber = phoneNumber;
        this.linkedAccounts = new HashMap<>();
        this.accountStatus = AccountStatus.ACTIVE;
    }
}

