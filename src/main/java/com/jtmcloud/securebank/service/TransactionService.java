package com.jtmcloud.securebank.service;

import com.jtmcloud.securebank.audit.Audited;
import com.jtmcloud.securebank.domain.model.Account;
import com.jtmcloud.securebank.domain.model.Transaction;
import com.jtmcloud.securebank.domain.model.TransactionStatus;
import com.jtmcloud.securebank.domain.model.TransactionType;
import com.jtmcloud.securebank.domain.repository.AccountRepository;
import com.jtmcloud.securebank.domain.repository.TransactionRepository;
import com.jtmcloud.securebank.dto.DepositRequest;
import com.jtmcloud.securebank.dto.TransferRequest;
import com.jtmcloud.securebank.dto.WithdrawalRequest;
import com.jtmcloud.securebank.exception.AccountNotFoundException;
import com.jtmcloud.securebank.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public Page<Transaction> history(UUID accountId, Authentication authentication, Pageable pageable) {
        Account account = accountService.getOwnedAccountOrThrow(accountId, authentication);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable);
    }

    /**
     * Virement inter-comptes. Le solde source est vérifié et débité sous verrou pessimiste
     * (SELECT ... FOR UPDATE) afin de garantir l'atomicité et d'empêcher tout dépassement
     * de solde en cas de virements concurrents sur le même compte.
     */
    @Transactional
    @Audited(action = "FUNDS_TRANSFER")
    public Transaction transfer(TransferRequest request, Authentication authentication) {
        Account source = accountRepository.findByIdForUpdate(request.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Compte source introuvable"));
        accountService.assertOwnership(source, authentication);

        Account destination = accountRepository.findByIban(request.destinationIban())
                .orElseThrow(() -> new AccountNotFoundException("IBAN destinataire introuvable"));

        if (source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Le compte source et destinataire doivent être différents.");
        }
        if (!source.isActive() || !destination.isActive()) {
            throw new IllegalStateException("Un des comptes impliqués est inactif.");
        }

        BigDecimal amount = request.amount();
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Solde insuffisant sur le compte " + source.getIban());
        }

        UUID transferReference = UUID.randomUUID();

        source.setBalance(source.getBalance().subtract(amount));
        accountRepository.save(source);

        Account lockedDestination = accountRepository.findByIdForUpdate(destination.getId())
                .orElseThrow(() -> new AccountNotFoundException("Compte destinataire introuvable"));
        lockedDestination.setBalance(lockedDestination.getBalance().add(amount));
        accountRepository.save(lockedDestination);

        Transaction debit = transactionRepository.save(Transaction.builder()
                .account(source)
                .transferReference(transferReference)
                .type(TransactionType.TRANSFER_OUT)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .balanceAfter(source.getBalance())
                .description(request.description())
                .counterpartyIban(destination.getIban())
                .build());

        transactionRepository.save(Transaction.builder()
                .account(lockedDestination)
                .transferReference(transferReference)
                .type(TransactionType.TRANSFER_IN)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .balanceAfter(lockedDestination.getBalance())
                .description(request.description())
                .counterpartyIban(source.getIban())
                .build());

        return debit;
    }

    /**
     * Dépôt de fonds sur un compte (simule un versement externe : virement entrant d'un
     * autre établissement, dépôt d'espèces en agence, etc.). Verrouillage pessimiste par
     * cohérence avec les autres opérations mouvementant un solde.
     */
    @Transactional
    @Audited(action = "FUNDS_DEPOSIT")
    public Transaction deposit(UUID accountId, DepositRequest request, Authentication authentication) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Compte introuvable: " + accountId));
        accountService.assertOwnership(account, authentication);
        if (!account.isActive()) {
            throw new IllegalStateException("Le compte est inactif.");
        }

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        return transactionRepository.save(Transaction.builder()
                .account(account)
                .transferReference(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.amount())
                .balanceAfter(account.getBalance())
                .description(request.description())
                .build());
    }

    /**
     * Retrait de fonds (simule un retrait en agence/DAB ou un virement sortant vers un
     * établissement tiers). Mêmes garanties d'atomicité et de non-dépassement de solde
     * que pour un virement.
     */
    @Transactional
    @Audited(action = "FUNDS_WITHDRAWAL")
    public Transaction withdraw(UUID accountId, WithdrawalRequest request, Authentication authentication) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Compte introuvable: " + accountId));
        accountService.assertOwnership(account, authentication);
        if (!account.isActive()) {
            throw new IllegalStateException("Le compte est inactif.");
        }
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Solde insuffisant sur le compte " + account.getIban());
        }

        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        return transactionRepository.save(Transaction.builder()
                .account(account)
                .transferReference(UUID.randomUUID())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .amount(request.amount())
                .balanceAfter(account.getBalance())
                .description(request.description())
                .build());
    }
}
