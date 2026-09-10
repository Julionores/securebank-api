package com.jtmcloud.securebank.dto;

import com.jtmcloud.securebank.domain.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String iban,
        BigDecimal balance,
        String currency,
        boolean active
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getIban(),
                account.getBalance(),
                account.getCurrency(),
                account.isActive()
        );
    }
}
