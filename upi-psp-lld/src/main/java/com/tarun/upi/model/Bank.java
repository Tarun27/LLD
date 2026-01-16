package com.tarun.upi.model;

import com.tarun.upi.model.enums.ServerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {

    String banknName;
    ServerStatus serverStatus;
}
