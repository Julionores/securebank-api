package com.jtmcloud.securebank.service;

import com.jtmcloud.securebank.domain.model.Account;
import com.jtmcloud.securebank.domain.repository.AccountRepository;
import com.jtmcloud.securebank.exception.AccountNotFoundException;
import com.jtmcloud.securebank.exception.UnauthorizedAccountAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<Account> listMyAccounts(Authentication authentication) {
        UUID ownerId = currentUserId(authentication);
        return accountRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Account getOwnedAccountOrThrow(UUID accountId, Authentication authentication) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Compte introuvable: " + accountId));
        assertOwnership(account, authentication);
        return account;
    }

    /**
     * Contrôle d'accès au niveau objet : un client ne peut consulter/mouvementer que ses
     * propres comptes, quel que soit l'identifiant fourni dans l'URL (OWASP A01 - Broken
     * Access Control / IDOR). Les administrateurs n'ont volontairement pas de bypass ici :
     * la supervision passe par des rapports dédiés, jamais par les endpoints clients.
     */
    public void assertOwnership(Account account, Authentication authentication) {
        UUID currentUserId = currentUserId(authentication);
        if (!account.getOwner().getId().equals(currentUserId)) {
            throw new UnauthorizedAccountAccessException(
                    "L'utilisateur " + authentication.getName() + " a tenté d'accéder au compte " + account.getId());
        }
    }

    private UUID currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.jtmcloud.securebank.domain.model.User user) {
            return user.getId();
        }
        throw new IllegalStateException("Principal inattendu: " + principal.getClass());
    }
}
