package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.formaterStatuslogging
import no.nav.helse.prosessering.tilK9Beskjed
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.helse.prosessering.v1.asynkron.deserialiserTilCleanup
import no.nav.helse.prosessering.v1.asynkron.process
import no.nav.helse.prosessering.v1.asynkron.serialiserTilData
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class CleanupStream(
    kafkaConfig: KafkaConfig,
    k9MellomlagringService: K9MellomlagringService,
    datoMottattEtter: ZonedDateTime
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(k9MellomlagringService, datoMottattEtter),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "CleanupV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(k9MellomlagringService: K9MellomlagringService, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fraCleanup = Topics.CLEANUP
            val tilK9DittnavVarsel = Topics.K9_DITTNAV_VARSEL

            builder
                .stream(fraCleanup.name, fraCleanup.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info(formaterStatuslogging(soknadId, "kjører cleanup"))
                        val cleanup = entry.deserialiserTilCleanup()

                        k9MellomlagringService.slettDokumeter(
                            urlBolks = cleanup.melding.dokumentUrls,
                            dokumentEier = DokumentEier(cleanup.melding.søker.fødselsnummer),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )

                        val k9beskjed = cleanup.tilK9Beskjed()
                        logger.info(formaterStatuslogging(soknadId, "sender K9Beskjed videre til ${tilK9DittnavVarsel.name} med eventId ${k9beskjed.eventId}"))
                        k9beskjed.serialiserTilData()
                    }
                }
                .to(tilK9DittnavVarsel.name, tilK9DittnavVarsel.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}