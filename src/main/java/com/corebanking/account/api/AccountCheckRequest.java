package com.corebanking.account.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AccountCheckRequest(
        @NotEmpty @Size(max = 200) List<@jakarta.validation.constraints.NotBlank String> ids
) {
}
