package com.corebanking.account.repo;

import com.corebanking.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findAllByIdIn(Collection<String> ids);
}
