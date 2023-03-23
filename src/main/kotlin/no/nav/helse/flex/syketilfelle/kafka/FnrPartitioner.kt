package no.nav.helse.flex.syketilfelle.kafka

import no.nav.helse.flex.syketilfelle.syketilfellebit.KafkaSyketilfellebit
import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster
import org.apache.kafka.common.InvalidRecordException
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.utils.Utils

abstract class FnrPartitioner : Partitioner {

    companion object {

        fun kalkulerPartisjon(keyBytes: ByteArray, numPartitions: Int): Int =
            Utils.toPositive(Utils.murmur2(keyBytes)) % (numPartitions)
    }

    override fun configure(configs: MutableMap<String, *>?) {}

    override fun close() {}

    override fun partition(
        topic: String?,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster?
    ): Int {
        val partitions: List<PartitionInfo> = cluster!!.partitionsForTopic(topic)
        val numPartitions: Int = partitions.size

        if (keyBytes == null || key !is String) {
            throw InvalidRecordException("All messages should have a valid key.")
        }

        return kalkulerPartisjon(keyBytes, numPartitions)
    }
}

class KafkaSyketilfellebitPartitioner : FnrPartitioner() {

    override fun partition(
        topic: String?,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster?
    ): Int {
        val bit = value as KafkaSyketilfellebit
        val actualKey: String = bit.fnr
        return super.partition(topic, actualKey, actualKey.toByteArray(), value, valueBytes, cluster)
    }
}
