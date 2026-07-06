package com.corebanking.account.api;

import com.corebanking.account.domain.Account;
import com.corebanking.account.domain.AccountStatus;
import com.corebanking.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock AccountService service;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AccountController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static final String CREATE_BODY = """
            {"accountId":"acc-001","ownerId":"cust-42","currency":"USD"}
            """;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        Account acc = new Account("acc-001", "cust-42", "USD", AccountStatus.ACTIVE,
                Instant.parse("2026-07-06T10:00:00Z"));
        when(service.create(eq("acc-001"), eq("cust-42"), eq("USD"))).thenReturn(acc);

        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/accounts/acc-001"))
                .andExpect(jsonPath("$.accountId").value("acc-001"))
                .andExpect(jsonPath("$.ownerId").value("cust-42"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(service).create(eq("acc-001"), eq("cust-42"), eq("USD"));
    }
}
