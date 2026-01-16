package com.tarun.upi.service;

import com.tarun.upi.exception.BankServerDownException;
import com.tarun.upi.exception.UPIException;
import com.tarun.upi.model.Bank;
import com.tarun.upi.model.User;
import com.tarun.upi.model.enums.ServerStatus;

import java.util.HashMap;
import java.util.Map;

public class BankService {

    Map<String, Bank> bankMap = new HashMap<>();

    public void addBank(String name, boolean status) {
        bankMap.put(name, new Bank(name, ServerStatus.fromStatus(status)));
    }

   public Bank getBank(String name) throws BankServerDownException, UPIException {
        if (!bankMap.containsKey(name)) throw new UPIException("Bank not registered");
        Bank b = bankMap.get(name);
        if (!b.getServerStatus().isUp()) throw new BankServerDownException(name + " server is down");
        return b;
    }

}
