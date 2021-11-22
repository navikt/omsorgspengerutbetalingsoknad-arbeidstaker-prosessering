package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.omsorgspengerKonfiguert
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertMelding
import no.nav.k9.søknad.Søknad
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.MeldingV1
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.json.JSONObject

data class Data(val rawJson: String)
data class Cleanup(val metadata: Metadata, val melding: PreprosessertMelding, val journalfort: Journalfort)
data class Journalfort(val journalpostId: String, val søknad: Søknad)

internal object Topics {

    val MOTTATT_V2 = Topic(
        name = "dusseldorf.privat-omsorgspengerutbetalingsoknad-arbeidstaker-mottatt-v2",
        serDes = SerDes()
    )
    val PREPROSESSERT = Topic(
        name = "dusseldorf.privat-omsorgspengerutbetalingsoknad-arbeidstaker-preprosessert",
        serDes = SerDes()
    )
    val CLEANUP = Topic(
        name = "dusseldorf.privat-omsorgspengerutbetalingsoknad-arbeidstaker-cleanup",
        serDes = SerDes()
    )
    val K9_DITTNAV_VARSEL = Topic(
        name = "dusseldorf.privat-k9-dittnav-varsel-beskjed",
        serDes = SerDes()
    )
}

internal fun TopicEntry.deserialiserTilCleanup(): Cleanup = mapper.readValue(data.rawJson)
internal fun TopicEntry.deserialiserTilMeldingV1(): MeldingV1 = mapper.readValue(data.rawJson)
internal fun TopicEntry.deserialiserTilPreprosessert(): PreprosessertMelding = mapper.readValue(data.rawJson)

internal fun Any.serialiserTilData() = Data(mapper.writeValueAsString(this))


class SerDes : Serializer<TopicEntry>, Deserializer<TopicEntry> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun deserialize(topic: String, entry: ByteArray): TopicEntry = TopicEntry(String(entry))
    override fun serialize(topic: String, entry: TopicEntry): ByteArray{
        return when(topic){
            Topics.K9_DITTNAV_VARSEL.name -> entry.data.rawJson.toByteArray()
            else -> entry.rawJson.toByteArray()
        }
    }
}

data class TopicEntry(val rawJson: String) {
    constructor(metadata: Metadata, data: Data) : this(
        JSONObject(
            mapOf(
                "metadata" to JSONObject(
                    mapOf(
                        "version" to metadata.version,
                        "correlationId" to metadata.correlationId
                    )
                ),
                "data" to JSONObject(data.rawJson)
            )
        ).toString()
    )

    private val entityJson = JSONObject(rawJson)
    private val metadataJson = requireNotNull(entityJson.getJSONObject("metadata"))
    private val dataJson = requireNotNull(entityJson.getJSONObject("data"))
    val metadata = Metadata(
        version = requireNotNull(metadataJson.getInt("version")),
        correlationId = requireNotNull(metadataJson.getString("correlationId"))
    )
    val data = Data(dataJson.toString())
}

internal data class Topic(
    val name: String,
    val serDes: SerDes
) {
    val keySerializer = StringSerializer()
    private val keySerde = Serdes.String()
    private val valueSerde = Serdes.serdeFrom(SerDes(), SerDes())
    val consumed = Consumed.with(keySerde, valueSerde)
    val produced = Produced.with(keySerde, valueSerde)
}

val mapper = jacksonObjectMapper().omsorgspengerKonfiguert()