package com.corebanking.account.api;

import java.util.List;
import java.util.Map;

public record AccountCheckResponse(
        Map<String, AccountResponse> accounts,
        List<String> missing
) {
}
