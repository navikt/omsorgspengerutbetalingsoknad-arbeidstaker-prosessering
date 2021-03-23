package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertArbeidstakerutbetalingMelding
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import no.nav.k9.søknad.omsorgspenger.utbetaling.arbeidstaker.OmsorgspengerUtbetalingSøknad as ArbeidstakerutbetalingSøknad

data class TopicEntry<V>(val metadata: Metadata, val data: V)

data class ArbeidstakerutbetalingCleanup(val metadata: Metadata, val melding: PreprosessertArbeidstakerutbetalingMelding, val journalførtMelding: ArbeidstakerutbetalingJournalfort)
data class ArbeidstakerutbetalingJournalfort(val journalpostId: String, val søknad: ArbeidstakerutbetalingSøknad)

internal data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object Topics {
    val MOTTATT = Topic(
        name = "privat-omp-utbetalingsoknad-arbeidstaker-mottatt",
        serDes = MottattSoknadSerDes()
    )
    val PREPROSSESERT = Topic(
        name = "privat-omp-utbetalingsoknad-arbeidstaker-preprossesert",
        serDes = PreprossesertSerDes()
    )
    val CLEANUP = Topic(
        name = "privat-omp-utbetalingsoknad-arbeidstaker-cleanup",
        serDes = CleanupSerDes()
    )
    val JOURNALFORT = Topic(
        name = "privat-omp-utbetalingsoknad-arbeidstaker-journalfort",
        serDes = JournalfortSerDes()
    )

}

internal abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
    protected val objectMapper = jacksonObjectMapper()
        .dusseldorfConfigured()
        .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
    override fun serialize(topic: String?, data: V): ByteArray? {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        }
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

private class MottattSoknadSerDes: SerDes<TopicEntry<ArbeidstakerutbetalingMelding>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<ArbeidstakerutbetalingMelding>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<ArbeidstakerutbetalingMelding>>(it)
        }
    }
}
private class PreprossesertSerDes: SerDes<TopicEntry<PreprosessertArbeidstakerutbetalingMelding>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprosessertArbeidstakerutbetalingMelding>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class CleanupSerDes: SerDes<TopicEntry<ArbeidstakerutbetalingCleanup>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<ArbeidstakerutbetalingCleanup>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class JournalfortSerDes: SerDes<TopicEntry<ArbeidstakerutbetalingJournalfort>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<ArbeidstakerutbetalingJournalfort>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}