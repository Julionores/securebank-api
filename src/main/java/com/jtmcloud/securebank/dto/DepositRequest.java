package com.jtmcloud.securebank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepositRequest(

        @NotNull
        @DecimalMin(value = "0.01", message = "Le montant doit être strictement positif.")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}
