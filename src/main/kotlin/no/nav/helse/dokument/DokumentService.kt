package no.nav.helse.dokument

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktørId
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private suspend fun lagreDokument(
        dokument: DokumentGateway.Dokument,
        aktørId: AktørId,
        correlationId: CorrelationId
    ) : URI {
        return dokumentGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId,
            aktørId = aktørId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf: ByteArray,
        aktørId: AktørId,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = pdf,
                contentType = "application/pdf",
                title = dokumentbeskrivelse
            ),
            aktørId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreSoknadsMelding(
        k9Format: Søknad,
        aktørId: AktørId,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ) : URI {
        logger.info("SKAL IKKE VISES I PROD! K9-Format som journalføres: {}", JsonUtils.toString(k9Format)) //TODO 11.03.2021 - Fjerne
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = Søknadsformat.somJson(k9Format),
                contentType = "application/json",
                title = dokumentbeskrivelse
            ),
            aktørId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        aktørId: AktørId,
        correlationId : CorrelationId
    ) {
        val urls = mutableListOf<URI>()
        urlBolks.forEach { urls.addAll(it) }

        logger.trace("Sletter ${urls.size} dokumenter")
        dokumentGateway.slettDokmenter(
            urls = urls,
            aktørId = aktørId,
            correlationId = correlationId
        )
    }
}

