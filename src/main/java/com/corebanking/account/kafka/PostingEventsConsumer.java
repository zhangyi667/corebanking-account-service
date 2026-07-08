package com.corebanking.account.kafka;

import com.corebanking.account.events.EventEnvelope;
import com.corebanking.account.events.PostingTransactionReceived;
import com.corebanking.account.service.BalanceApplier;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@EnableKafka
public class PostingEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PostingEventsConsumer.class);

    private final ObjectMapper mapper;
    private final BalanceApplier applier;

    public PostingEventsConsumer(ObjectMapper mapper, BalanceApplier applier) {
        this.mapper = mapper;
        this.applier = applier;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.posting-transaction}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "postingEventsListenerContainerFactory"
    )
    public void onPostingTransactionReceived(EventEnvelope envelope) {
        PostingTransactionReceived posting =
                mapper.convertValue(envelope.payload(), PostingTransactionReceived.class);

        BalanceApplier.Result result = applier.apply(posting);
        if (result == BalanceApplier.Result.DUPLICATE) {
            log.info("posting.transaction.received DUPLICATE; idempotencyKey={} postingId={} — skipped",
                    posting.idempotencyKey(), posting.postingId());
        } else {
            log.info("posting.transaction.received APPLIED; idempotencyKey={} postingId={} legs={}",
                    posting.idempotencyKey(), posting.postingId(), posting.legs().size());
        }
    }
}
