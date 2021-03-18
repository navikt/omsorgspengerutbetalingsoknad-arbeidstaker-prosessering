package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import no.nav.helse.prosessering.v1.asynkron.TopicUse
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.json.JSONObject
import org.slf4j.LoggerFactory

class AleneOmOmsorgenProducer(
    val kafkaConfig: KafkaConfig
) {

    private companion object {
        private val NAME = "AleneOmOmsorgenProducer"
        private val K9_RAPID_V2_TOPIC = TopicUse(
            name = Topics.K9_RAPID_V2.name,
            valueSerializer = SøknadSerializer()
        )
        private val logger = LoggerFactory.getLogger(AleneOmOmsorgenProducer::class.java)
    }


    private val producer = KafkaProducer(
        kafkaConfig.producer(NAME),
        K9_RAPID_V2_TOPIC.keySerializer(),
        K9_RAPID_V2_TOPIC.valueSerializer
    )

    internal fun produce(
        melding: ArbeidstakerutbetalingMelding,
        metadata: Metadata
    ) {
        val recordMetaData = producer.send(
            ProducerRecord(
                K9_RAPID_V2_TOPIC.name,
                melding.søknadId,
                TopicEntry(
                    metadata = metadata,
                    data = JSONObject(melding)
                )
            )
        ).get()
    }

    internal fun stop() = producer.close()

}


private class SøknadSerializer : Serializer<TopicEntry<JSONObject>> {
    override fun serialize(topic: String, data: TopicEntry<JSONObject>) : ByteArray {
        val metadata = JSONObject()
            .put("correlationId", data.metadata.correlationId)
            .put("requestId", data.metadata.requestId)
            .put("version", data.metadata.version)

        return JSONObject()
            .put("metadata", metadata)
            .put("data", data.data)
            .toString()
            .toByteArray()
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}