package com.corebanking.account.repo;

import com.corebanking.account.domain.Balance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, String> {

    // Row lock on the balance we're about to mutate. Two concurrent postings
    // touching the same account serialize here; unrelated accounts run parallel.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Balance> findByAccountId(String accountId);
}
