package no.nav.helse.flex.syketilfelle.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class TestKafkaConfig(
    private val aivenKafkaConfig: AivenKafkaConfig,
) {
    @Bean
    fun kafkaProducer(): KafkaProducer<String, String> {
        val config =
            mapOf(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to 10,
                ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 100,
            ) + aivenKafkaConfig.commonConfig()
        return KafkaProducer(config)
    }

    @Bean
    fun syketilfelleBitConsumer() = KafkaConsumer<String, String>(consumerConfig("bit-group-id"))

    @Bean
    fun nullableStringConsumer() = KafkaConsumer<String, String?>(consumerConfig("testing"))

    @Bean
    fun juridiskVurderingKafkaConsumer() = KafkaConsumer<String, String>(consumerConfig("juridisk-group-id"))

    private fun consumerConfig(groupId: String) =
        mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ) + aivenKafkaConfig.commonConfig()
}
