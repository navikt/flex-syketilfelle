package no.nav.helse.flex.syketilfelle.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.listener.*
import org.springframework.stereotype.Component
import org.springframework.util.backoff.ExponentialBackOff
import java.lang.Exception
import no.nav.helse.flex.syketilfelle.logger as slf4jLogger

@Component
class KafkaErrorHandler :
    DefaultErrorHandler(
        ExponentialBackOff(1000L, 1.5).apply {
            // 8 minutter, som er mindre enn max.poll.interval.ms på 10 minutter.
            maxInterval = 60_000L * 8
        },
    ) {
    // Bruker aliased logger for unngå kollisjon med CommonErrorHandler.logger(): LogAccessor.
    val log = slf4jLogger()

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        records.forEach { record ->
            log.error(
                "Feil i prossessering av record med offset: ${record.offset()}, partition: ${record.partition()} på" +
                    "topic ${record.topic()}",
                thrownException,
            )
        }
        if (records.isEmpty()) {
            log.error("Feil i listener uten noen records", thrownException)
        }
        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        records: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        records.forEach { record ->
            log.error(
                "Feil i prossessering av record med offset: ${record.offset()}, partition: ${record.partition()} på" +
                    "topic ${record.topic()}",
                thrownException,
            )
        }
        if (records.isEmpty()) {
            log.error("Feil i listener uten noen records", thrownException)
        }
        super.handleBatch(thrownException, records, consumer, container, invokeListener)
    }
}
