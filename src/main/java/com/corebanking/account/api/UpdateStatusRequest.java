package com.corebanking.account.api;

import com.corebanking.account.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull AccountStatus status) {
}
