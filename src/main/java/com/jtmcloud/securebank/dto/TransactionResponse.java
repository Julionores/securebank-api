package com.jtmcloud.securebank.dto;

import com.jtmcloud.securebank.domain.model.Transaction;
import com.jtmcloud.securebank.domain.model.TransactionStatus;
import com.jtmcloud.securebank.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyIban,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getType(), t.getStatus(), t.getAmount(),
                t.getBalanceAfter(), t.getCounterpartyIban(), t.getDescription(), t.getCreatedAt()
        );
    }
}
