package com.jtmcloud.securebank.service;

import com.jtmcloud.securebank.domain.model.Account;
import com.jtmcloud.securebank.domain.model.Role;
import com.jtmcloud.securebank.domain.model.User;
import com.jtmcloud.securebank.domain.repository.AccountRepository;
import com.jtmcloud.securebank.domain.repository.TransactionRepository;
import com.jtmcloud.securebank.dto.DepositRequest;
import com.jtmcloud.securebank.dto.TransferRequest;
import com.jtmcloud.securebank.dto.WithdrawalRequest;
import com.jtmcloud.securebank.exception.AccountNotFoundException;
import com.jtmcloud.securebank.exception.InsufficientFundsException;
import com.jtmcloud.securebank.exception.UnauthorizedAccountAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du cœur métier des virements : garantit qu'un virement ne peut jamais
 * faire passer un solde en négatif et qu'un utilisateur ne peut jamais mouvementer un
 * compte qui ne lui appartient pas.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private AccountService accountService;
    private TransactionService transactionService;

    private User owner;
    private User stranger;
    private Account sourceAccount;
    private Account destinationAccount;
    private Authentication ownerAuth;
    private Authentication strangerAuth;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
        transactionService = new TransactionService(accountRepository, transactionRepository, accountService);

        owner = User.builder().id(UUID.randomUUID()).email("owner@securebank.test").role(Role.ROLE_CUSTOMER).build();
        stranger = User.builder().id(UUID.randomUUID()).email("stranger@securebank.test").role(Role.ROLE_CUSTOMER).build();

        sourceAccount = Account.builder()
                .id(UUID.randomUUID()).iban("CM21SOURCE0000000000")
                .owner(owner).balance(new BigDecimal("100.0000")).currency("XAF").active(true).build();

        destinationAccount = Account.builder()
                .id(UUID.randomUUID()).iban("CM21DEST00000000000")
                .owner(stranger).balance(new BigDecimal("10.0000")).currency("XAF").active(true).build();

        ownerAuth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());
        strangerAuth = new UsernamePasswordAuthenticationToken(stranger, null, stranger.getAuthorities());
    }

    @Test
    void transfer_debitsSourceAndCreditsDestination() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIban(destinationAccount.getIban())).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByIdForUpdate(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(), destinationAccount.getIban(), new BigDecimal("30.00"), "test");

        transactionService.transfer(request, ownerAuth);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("70.0000");
        assertThat(destinationAccount.getBalance()).isEqualByComparingTo("40.0000");
    }

    @Test
    void transfer_rejectsWhenBalanceInsufficient() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIban(destinationAccount.getIban())).thenReturn(Optional.of(destinationAccount));

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(), destinationAccount.getIban(), new BigDecimal("999.00"), null);

        assertThatThrownBy(() -> transactionService.transfer(request, ownerAuth))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_rejectsWhenCallerIsNotAccountOwner() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(), destinationAccount.getIban(), new BigDecimal("10.00"), null);

        // "stranger" n'est pas propriétaire de sourceAccount -> doit être bloqué avant tout débit.
        assertThatThrownBy(() -> transactionService.transfer(request, strangerAuth))
                .isInstanceOf(UnauthorizedAccountAccessException.class);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("100.0000");
    }

    @Test
    void transfer_rejectsUnknownDestinationIban() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIban("CM21UNKNOWN000000000")).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(), "CM21UNKNOWN000000000", new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> transactionService.transfer(request, ownerAuth))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void deposit_creditsAccount() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.deposit(sourceAccount.getId(), new DepositRequest(new BigDecimal("50.00"), "top-up"), ownerAuth);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("150.0000");
    }

    @Test
    void deposit_rejectsWhenCallerIsNotAccountOwner() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.deposit(
                sourceAccount.getId(), new DepositRequest(new BigDecimal("50.00"), null), strangerAuth))
                .isInstanceOf(UnauthorizedAccountAccessException.class);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("100.0000");
    }

    @Test
    void withdraw_debitsAccount() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.withdraw(sourceAccount.getId(), new WithdrawalRequest(new BigDecimal("40.00"), "cash-out"), ownerAuth);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("60.0000");
    }

    @Test
    void withdraw_rejectsWhenBalanceInsufficient() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.withdraw(
                sourceAccount.getId(), new WithdrawalRequest(new BigDecimal("999.00"), null), ownerAuth))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("100.0000");
    }

    @Test
    void withdraw_rejectsWhenCallerIsNotAccountOwner() {
        when(accountRepository.findByIdForUpdate(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.withdraw(
                sourceAccount.getId(), new WithdrawalRequest(new BigDecimal("10.00"), null), strangerAuth))
                .isInstanceOf(UnauthorizedAccountAccessException.class);
    }
}
