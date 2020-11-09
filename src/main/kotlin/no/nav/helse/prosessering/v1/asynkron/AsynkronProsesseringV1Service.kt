package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentService
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.CleanupStream
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.ArbeidstakerutbetalingJournalforingsStream
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosseseringStream
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    preprosseseringV1Service: PreprosseseringV1Service,
    joarkGateway: JoarkGateway,
    dokumentService: DokumentService,
    datoMottattEtter: ZonedDateTime
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    private val arbeidstakerutbetalingPreprosseseringStream = PreprosseseringStream(
        kafkaConfig = kafkaConfig,
        preprosseseringV1Service = preprosseseringV1Service,
        datoMottattEtter = datoMottattEtter
    )

    private val arbeidstakerutbetalingJournalforingsStream = ArbeidstakerutbetalingJournalforingsStream(
        kafkaConfig = kafkaConfig,
        joarkGateway = joarkGateway,
        datoMottattEtter = datoMottattEtter
    )

    private val arbeidstakerutbetalingCleanupStream = CleanupStream(
        kafkaConfig = kafkaConfig,
        dokumentService = dokumentService,
        datoMottattEtter = datoMottattEtter
    )

    private val healthChecks = setOf(
        arbeidstakerutbetalingPreprosseseringStream.healthy,
        arbeidstakerutbetalingJournalforingsStream.healthy,
        arbeidstakerutbetalingCleanupStream.healthy
    )

    private val isReadyChecks = setOf(
        arbeidstakerutbetalingPreprosseseringStream.ready,
        arbeidstakerutbetalingJournalforingsStream.ready,
        arbeidstakerutbetalingCleanupStream.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")
        arbeidstakerutbetalingPreprosseseringStream.stop()
        arbeidstakerutbetalingJournalforingsStream.stop()
        arbeidstakerutbetalingCleanupStream.stop()

        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}
