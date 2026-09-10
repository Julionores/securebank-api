package com.jtmcloud.securebank.controller;

import com.jtmcloud.securebank.domain.model.Transaction;
import com.jtmcloud.securebank.dto.DepositRequest;
import com.jtmcloud.securebank.dto.TransactionResponse;
import com.jtmcloud.securebank.dto.TransferRequest;
import com.jtmcloud.securebank.dto.WithdrawalRequest;
import com.jtmcloud.securebank.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication
    ) {
        Transaction transaction = transactionService.transfer(request, authentication);
        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }

    @PostMapping("/accounts/{accountId}/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        Transaction transaction = transactionService.deposit(accountId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }

    @PostMapping("/accounts/{accountId}/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication
    ) {
        Transaction transaction = transactionService.withdraw(accountId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public Page<TransactionResponse> history(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return transactionService.history(accountId, authentication, pageable)
                .map(TransactionResponse::from);
    }
}
