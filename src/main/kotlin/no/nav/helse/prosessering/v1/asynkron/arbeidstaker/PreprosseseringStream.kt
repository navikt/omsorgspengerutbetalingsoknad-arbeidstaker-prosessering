package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.helse.prosessering.v1.asynkron.deserialiserTilMeldingV1
import no.nav.helse.prosessering.v1.asynkron.process
import no.nav.helse.prosessering.v1.asynkron.serialiserTilData
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class PreprosseseringStream(
    preprosseseringV1Service: PreprosseseringV1Service,
    kafkaConfig: KafkaConfig,
    datoMottattEtter: ZonedDateTime
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosseseringV1Service, datoMottattEtter),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {

        private const val NAME = "PreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosseseringV1Service: PreprosseseringV1Service, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fromMottatt = Topics.MOTTATT
            val tilPreprosessert = Topics.PREPROSESSERT

            builder
                .stream(fromMottatt.name, fromMottatt.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Preprosesserer søknad om utbetaling av omsorgspenger for arbeidstakere.")

                        val preprossesertMelding = preprosseseringV1Service.preprosseser(
                            melding = entry.deserialiserTilMeldingV1(),
                            metadata = entry.metadata
                        )
                        logger.info("Preprossesering ferdig.")
                        preprossesertMelding.serialiserTilData()
                    }
                }
                .to(tilPreprosessert.name, tilPreprosessert.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
