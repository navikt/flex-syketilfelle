package no.nav.helse.flex.syketilfelle.kafka

import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurderingKafkaDto
import no.nav.helse.flex.syketilfelle.syketilfellebit.KafkaSyketilfellebit
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

private const val JAVA_KEYSTORE = "JKS"
private const val PKCS12 = "PKCS12"

@Configuration
class AivenKafkaConfig(
    @param:Value("\${KAFKA_BROKERS}") private val kafkaBrokers: String,
    @param:Value("\${KAFKA_TRUSTSTORE_PATH}") private val kafkaTruststorePath: String,
    @param:Value("\${aiven-kafka.auto-offset-reset}") private val kafkaAutoOffsetReset: String,
    @param:Value("\${aiven-kafka.security-protocol}") private val kafkaSecurityProtocol: String,
    @param:Value("\${KAFKA_CREDSTORE_PASSWORD}") private val kafkaCredstorePassword: String,
    @param:Value("\${KAFKA_KEYSTORE_PATH}") private val kafkaKeystorePath: String,
) {
    fun commonConfig() =
        mapOf(
            BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
        ) + securityConfig()

    private fun securityConfig() =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to kafkaSecurityProtocol,
            // Disables server host name verification.
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )

    private fun consumerConfig(groupId: String) =
        mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaAutoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000",
        ) + commonConfig()

    private fun producerConfig() =
        mapOf(
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 10,
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 100,
        ) + commonConfig()

    private fun <T> kafkaProducer(valueSerializer: Serializer<T>) = KafkaProducer(producerConfig(), StringSerializer(), valueSerializer)

    @Bean
    fun aivenKafkaListenerContainerFactory(kafkaErrorHandler: KafkaErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        lagListenerContainerFactory("flex-syketilfelle", kafkaErrorHandler)

    @Bean
    fun syketilfelleKafkaListenerContainerFactory(
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        // Overtar consumer group fra syfosyketilfelle.
        return lagListenerContainerFactory("syfosyketilfelle-consumer", kafkaErrorHandler)
    }

    @Bean
    fun producer(): KafkaProducer<String, KafkaSyketilfellebit?> = kafkaProducer(JacksonKafkaSerializer())

    @Bean
    fun juridiskVurderingProducer(): KafkaProducer<String, JuridiskVurderingKafkaDto> = kafkaProducer(JacksonKafkaSerializer())

    private fun lagListenerContainerFactory(
        groupId: String,
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(DefaultKafkaConsumerFactory(consumerConfig(groupId)))
        factory.setCommonErrorHandler(kafkaErrorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }
}
