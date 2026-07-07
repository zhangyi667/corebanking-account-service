package com.corebanking.account.repo;

import com.corebanking.account.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // Native SKIP LOCKED: multiple publisher workers pull disjoint batches
    // without blocking each other. FOR UPDATE ensures the reader holds the
    // row until it marks sent, so a re-poll won't pick the same row twice.
    @Query(value = "SELECT * FROM outbox_event WHERE sent_at IS NULL " +
            "ORDER BY id ASC LIMIT :#{#pageable.pageSize} " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> pickUnsent(Pageable pageable);

    long countBySentAtIsNull();
}
