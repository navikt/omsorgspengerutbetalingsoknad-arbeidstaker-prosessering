package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.joark.JoarkGateway
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v1.PreprosesseringV1Service
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.ArbeidstakerutbetalingJournalforingsStream
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.CleanupStream
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosesseringStream
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    preprosseseringV1Service: PreprosesseringV1Service,
    joarkGateway: JoarkGateway,
    k9MellomlagringService: K9MellomlagringService,
    datoMottattEtter: ZonedDateTime
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    private val arbeidstakerutbetalingPreprosesseringStream = PreprosesseringStream(
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
        k9MellomlagringService = k9MellomlagringService,
        datoMottattEtter = datoMottattEtter
    )

    private val healthChecks = setOf(
        arbeidstakerutbetalingPreprosesseringStream.healthy,
        arbeidstakerutbetalingJournalforingsStream.healthy,
        arbeidstakerutbetalingCleanupStream.healthy
    )

    private val isReadyChecks = setOf(
        arbeidstakerutbetalingPreprosesseringStream.ready,
        arbeidstakerutbetalingJournalforingsStream.ready,
        arbeidstakerutbetalingCleanupStream.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")
        arbeidstakerutbetalingPreprosesseringStream.stop()
        arbeidstakerutbetalingJournalforingsStream.stop()
        arbeidstakerutbetalingCleanupStream.stop()

        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}
