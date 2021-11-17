package no.nav.helse.prosessering

import no.nav.helse.prosessering.v1.asynkron.Cleanup
import java.util.*

const val DAGER_SYNLIG_K9BESKJED: Long = 7
const val TEKST_K9BESKJED: String = "Søknad om utbetaling av omsorgspenger er mottatt."
const val YTELSE_K9BESKJED: String = "OMSORGSPENGER_UT_ARBEIDSTAKER"

data class K9Beskjed(
    val metadata: Metadata,
    val grupperingsId: String,
    val ytelse: String = YTELSE_K9BESKJED,
    val tekst: String = TEKST_K9BESKJED,
    val dagerSynlig: Long = DAGER_SYNLIG_K9BESKJED,
    val link: String? = null,
    val søkerFødselsnummer: String,
    val eventId: String
)

fun Cleanup.tilK9Beskjed() = K9Beskjed(
    metadata = metadata,
    grupperingsId = melding.soknadId,
    link = null,
    søkerFødselsnummer = melding.søker.fødselsnummer,
    eventId = UUID.randomUUID().toString()
)