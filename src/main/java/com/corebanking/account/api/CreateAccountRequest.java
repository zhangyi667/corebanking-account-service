package com.corebanking.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 64) String accountId,
        @NotBlank @Size(max = 64) String ownerId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "currency must be ISO-4217 3-letter") String currency
) {
}
