package com.jtmcloud.securebank.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Écriture comptable immuable rattachée à un compte. Une opération de virement génère
 * deux lignes (TRANSFER_OUT côté débiteur, TRANSFER_IN côté créditeur) afin de conserver
 * une piste d'audit complète et de ne jamais muter un enregistrement existant.
 */
@Entity
@Table(name = "transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Référence commune aux deux écritures d'un même virement (corrélation). */
    @Column(nullable = false)
    private UUID transferReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 255)
    private String description;

    /** IBAN de la contrepartie, à des fins d'affichage (pas de relation forcée si compte externe). */
    @Column(length = 34)
    private String counterpartyIban;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
