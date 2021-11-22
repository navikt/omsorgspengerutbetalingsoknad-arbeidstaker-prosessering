package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.Dokument
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.k9mellomlagring.Søknadsformat
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertMelding
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.reportMetrics
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.MeldingV1
import org.slf4j.LoggerFactory

internal class PreprosesseringV1Service(
    private val pdfV1Generator: PdfV1Generator,
    private val k9MellomlagringService: K9MellomlagringService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosesseringV1Service::class.java)
    }

    internal suspend fun preprosesser(
        melding: MeldingV1,
        metadata: Metadata
    ): PreprosessertMelding {
        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentEier(melding.søker.fødselsnummer)

        logger.info("Genererer Oppsummerings-PDF av søknaden.")
        val oppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val soknadOppsummeringPdfUrl = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
              eier = dokumentEier,
              content = oppsummeringPdf,
              contentType = "application/pdf",
              title = "Søknad om utbetaling av omsorgspenger - Arbeidstaker"
            ),
            correlationId = correlationId
        )

        logger.info("Mellomlagrer Oppsummerings-JSON")
        val søknadJsonUrl = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = Søknadsformat.somJson(melding.k9Format),
                contentType = "application/json",
                title = "Søknad om utbetaling av omsorgspenger - Arbeidstaker som JSON"
            ),
            correlationId = correlationId
        )

        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                søknadJsonUrl
            )
        )

        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it)) }
        }

        logger.info("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        val preprosessertArbeidstakerutbetalingMelding = PreprosessertMelding(
            melding = melding,
            dokumentUrls = komplettDokumentUrls.toList()
        )

        melding.reportMetrics()
        return preprosessertArbeidstakerutbetalingMelding
    }
}