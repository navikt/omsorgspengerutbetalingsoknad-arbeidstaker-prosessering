package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import java.util.*

private const val k9JoarkBasePath = "/k9-joark-mock"
private const val k9MellomlagringBasePath = "/k9-mellomlagring-mock"


internal fun WireMockServer.stubLagreDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9MellomlagringBasePath.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("Location", "${getK9MellomlagringBaseUrl()}/v1/dokument/${UUID.randomUUID()}")
                .withStatus(201)
        )
    )
    return this
}

internal fun WireMockServer.stubSlettDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.delete(WireMock.urlPathMatching(".*$k9MellomlagringBasePath.*")).willReturn(
            WireMock.aResponse()
                .withStatus(204)
        )
    )
    return this
}

internal fun WireMockServer.stubJournalfor(responseCode: Int = 201): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$k9JoarkBasePath/v1/omsorgspengeutbetaling/journalforing")
        )
            .withQueryParam("arbeidstype", equalTo("arbeidstaker"))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "journal_post_id" : "9101112"
                            }
                            """.trimIndent()
                    )
                    .withStatus(responseCode)
            )
    )
    return this
}


private fun WireMockServer.stubHealthEndpoint(
    path: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path")).willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
    return this
}

internal fun WireMockServer.stubK9MellomlagringHealth() = stubHealthEndpoint("$k9MellomlagringBasePath/health")
internal fun WireMockServer.stubK9JoarkHealth() = stubHealthEndpoint("$k9JoarkBasePath/health")
internal fun WireMockServer.getk9JoarkBaseUrl() = baseUrl() + k9JoarkBasePath
internal fun WireMockServer.getK9MellomlagringBaseUrl() = baseUrl() + k9MellomlagringBasePath