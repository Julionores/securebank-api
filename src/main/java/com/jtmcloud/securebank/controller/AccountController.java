package com.jtmcloud.securebank.controller;

import com.jtmcloud.securebank.dto.AccountResponse;
import com.jtmcloud.securebank.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Comptes")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> listMyAccounts(Authentication authentication) {
        return accountService.listMyAccounts(authentication).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId, Authentication authentication) {
        return AccountResponse.from(accountService.getOwnedAccountOrThrow(accountId, authentication));
    }
}
