package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.CorrelationId
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.formaterStatuslogging
import no.nav.helse.prosessering.v1.asynkron.*
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class ArbeidstakerutbetalingJournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig,
    datoMottattEtter: ZonedDateTime
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway, datoMottattEtter),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "JournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fraPreprosessert = Topics.PREPROSESSERT
            val tilCleanup = Topics.CLEANUP

            val mapValues = builder
                .stream(fraPreprosessert.name, fraPreprosessert.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info(formaterStatuslogging(soknadId, "journalføres"))
                        val preprosessertMelding = entry.deserialiserTilPreprosessert()

                        logger.info("Journalfører dokumenter: {}", preprosessertMelding.dokumentId)
                        val journaPostId = joarkGateway.journalfør(
                            preprosessertMelding = preprosessertMelding,
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )

                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalpostId}.")

                        val journalfort = Journalfort(
                            journalpostId = journaPostId.journalpostId,
                            søknad = preprosessertMelding.k9Format
                        )

                        Cleanup(
                            metadata = entry.metadata,
                            melding = preprosessertMelding,
                            journalfort = journalfort
                        ).serialiserTilData()
                    }
                }
            mapValues
                .to(tilCleanup.name, tilCleanup.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}