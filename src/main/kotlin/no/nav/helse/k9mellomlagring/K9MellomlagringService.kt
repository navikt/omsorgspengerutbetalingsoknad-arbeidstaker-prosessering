package no.nav.helse.k9mellomlagring

import no.nav.helse.CorrelationId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger(K9MellomlagringService::class.java)

class K9MellomlagringService(
    private val k9MellomlagringGateway: K9MellomlagringGateway
) {
    internal suspend fun lagreDokument(
        dokument: Dokument,
        correlationId: CorrelationId
    ) : URI {
        return k9MellomlagringGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId
        ).first()
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        dokumentEier: DokumentEier,
        correlationId : CorrelationId
    ) {
        val urls = mutableListOf<URI>()
        urlBolks.forEach { urls.addAll(it) }
        logger.trace("Sletter ${urls.size} dokumenter")
        k9MellomlagringGateway.slettDokmenter(
            urls = urls,
            dokumentEier = dokumentEier,
            correlationId = correlationId
        )

    }

}