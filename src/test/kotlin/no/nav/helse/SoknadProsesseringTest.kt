package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.SøknadUtils.defaultSøknad
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals


class SoknadProsesseringTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(SoknadProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubK9MellomlagringHealth()
            .stubK9JoarkHealth()
            .stubJournalfor()
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaProducer = kafkaEnvironment.arbeidstakerutbetalingMeldingProducer()
        private val cleanupKonsumer = kafkaEnvironment.cleanupKonsumer()
        private val k9DittnavVarselKonsumer = kafkaEnvironment.k9DittnavVarselKonsumer()

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val dNummerA = "55125314561"

        private var engine = newEngine(kafkaEnvironment).apply {
            start(wait = true)
        }

        private fun getConfig(kafkaEnvironment: KafkaEnvironment?): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaEnvironment = kafkaEnvironment
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaEnvironment: KafkaEnvironment?) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })

        private fun stopEngine() = engine.stop(5, 60, TimeUnit.SECONDS)

        internal fun restartEngine() {
            stopEngine()
            CollectorRegistry.defaultRegistry.clear()
            engine = newEngine(kafkaEnvironment)
            engine.start(wait = true)
        }

        @BeforeAll
        @JvmStatic
        fun buildUp() {
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            kafkaProducer.close()
            cleanupKonsumer.close()
            stopEngine()
            kafkaEnvironment.tearDown()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(fødselsnummer = gyldigFodselsnummerA)
        )

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        kafkaProducer.leggTilMottak(melding)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()

        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertCleanupFormat()

        k9DittnavVarselKonsumer
            .hentK9Beskjed(melding.søknadId, maxWaitInSeconds = 60)
            .assertK9Beskjed(melding)
    }

    private fun readyGir200HealthGir503() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/health") {}.apply {
                    assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                }
            }
        }
    }

    @Test
    fun `Melding som gjeder søker med D-nummer`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(fødselsnummer = dNummerA)
        )

        kafkaProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertCleanupFormat()

        k9DittnavVarselKonsumer
            .hentK9Beskjed(melding.søknadId)
            .assertK9Beskjed(melding)
    }

    @Test
    fun `Forvent riktig format på cleanup melding`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(fødselsnummer = gyldigFodselsnummerA)
        )

        kafkaProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertCleanupFormat()

        k9DittnavVarselKonsumer
            .hentK9Beskjed(melding.søknadId)
            .assertK9Beskjed(melding)
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
