package no.nav.helse.joark

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertMelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.ZonedDateTime

class JoarkGateway(
    baseUrl: URI,
    private val accessTokenClient: AccessTokenClient,
    private val journalforeScopes: Set<String>
) : HealthCheck {
    private companion object {
        private const val JOURNALFORING_OPERATION = "journalforing"
        private val logger: Logger = LoggerFactory.getLogger(JoarkGateway::class.java)
    }

    private val arbeidstakerUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "omsorgspengeutbetaling", "journalforing"),
        queryParameters = mapOf("arbeidstype" to listOf("arbeidstaker"))
    ).toString()

    private val objectMapper = configuredObjectMapper()
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)


    override suspend fun check(): Result {
        return try {
            accessTokenClient.getAccessToken(journalforeScopes)
            Healthy("JoarkGateway", "Henting av access token for journalføring OK.")
        } catch (cause: Throwable) {
            logger.error("Feil ved henting av access token for journalføring", cause)
            UnHealthy("JoarkGateway", "Henting av access token for journalføring feilet.")
        }
    }

    suspend fun journalfør(
        preprosessertMelding: PreprosessertMelding,
        correlationId: CorrelationId,
    ): JournalPostId {

        val authorizationHeader = cachedAccessTokenClient.getAccessToken(journalforeScopes).asAuthoriationHeader()

        val joarkRequest = JoarkRequest(
            norskIdent = preprosessertMelding.søker.fødselsnummer,
            mottatt = preprosessertMelding.mottatt,
            dokumenteId = preprosessertMelding.dokumentId,
            sokerNavn = JoarkNavn(
                fornavn = preprosessertMelding.søker.fornavn,
                mellomnanvn= preprosessertMelding.søker.mellomnavn,
                etternavn = preprosessertMelding.søker.etternavn
            )
        )

        val body = objectMapper.writeValueAsBytes(joarkRequest)
        val contentStream = { ByteArrayInputStream(body) }

        val httpRequest = arbeidstakerUrl
            .httpPost()
            .body(contentStream)
            .header(
                HttpHeaders.XCorrelationId to correlationId.value,
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.ContentType to "application/json",
                HttpHeaders.Accept to "application/json"
            )

        val (request, response, result) = Operation.monitored(
            app = "omsorgspengerutbetalingsoknad-arbeidstaker-prosessering",
            operation = JOURNALFORING_OPERATION,
            resultResolver = { 201 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        return result.fold(
            { success -> objectMapper.readValue(success) },
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error(error.toString())
                throw HttpError(response.statusCode, "Feil ved jorunalføring.")
            }
        )
    }

    private fun configuredObjectMapper(): ObjectMapper {
        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.registerModule(JavaTimeModule())
        return objectMapper
    }
}

private data class JoarkRequest(
    @JsonProperty("norsk_ident") val norskIdent: String,
    val mottatt: ZonedDateTime,
    @JsonProperty("dokument_id")
    val dokumenteId: List<List<String>>,
    @JsonProperty("soker_navn")
    val sokerNavn: JoarkNavn
)

data class JournalPostId(@JsonProperty("journal_post_id") val journalpostId: String)

data class JoarkNavn(
    val fornavn: String,
    val mellomnanvn: String?,
    val etternavn: String
)
