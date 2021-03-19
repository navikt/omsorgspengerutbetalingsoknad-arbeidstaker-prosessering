package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktørId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.AleneOmOmsorgenProducer
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertArbeidstakerutbetalingMelding
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.reportMetrics
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding
import org.slf4j.LoggerFactory

internal class PreprosseseringV1Service(
    private val aleneOmOmsorgenProducer: AleneOmOmsorgenProducer,
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: ArbeidstakerutbetalingMelding,
        metadata: Metadata
    ): PreprosessertArbeidstakerutbetalingMelding {
        val søknadId = SoknadId(melding.søknadId)
        logger.info("Preprosseserer $søknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val søkerAktørId = AktørId(melding.søker.aktørId)

        logger.info("Genererer Oppsummerings-PDF av søknaden.")
        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.info("Generering av Oppsummerings-PDF OK.")

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            aktørId = søkerAktørId,
            correlationId = correlationId,
            dokumentbeskrivelse = "Søknad om utbetaling av omsorgspenger - Arbeidstaker"
        )
        logger.info("Mellomlagring av Oppsummerings-PDF OK")

        logger.info("Mellomlagrer Oppsummerings-JSON")

        val soknadJsonUrl = dokumentService.lagreSoknadsMelding(
            melding = melding,
            aktørId = søkerAktørId,
            correlationId = correlationId,
            dokumentbeskrivelse = "Søknad om utbetaling av omsorgspenger - Arbeidstaker som JSON"
        )
        logger.info("Mellomlagrer Oppsummerings-JSON OK.")


        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                soknadJsonUrl
            )
        )

        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it)) }
        }

        logger.info("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        val preprosessertArbeidstakerutbetalingMelding = PreprosessertArbeidstakerutbetalingMelding(
            melding = melding,
            dokumentUrls = komplettDokumentUrls.toList(),
            søkerAktørId = søkerAktørId
        )

        if(melding.skalRegistrereAleneOmOmsorgenBarn()){
            logger.info("Registrerer barn med alene om omsorgen.")
            aleneOmOmsorgenProducer.leggPåKø(melding, metadata)
        }

        melding.reportMetrics()
        return preprosessertArbeidstakerutbetalingMelding
    }
}

private fun ArbeidstakerutbetalingMelding.skalRegistrereAleneOmOmsorgenBarn(): Boolean {
    barn.forEach {
        if(it.aleneOmOmsorgen) return true
    }
    return false
}
