package no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.Ansettelseslengde
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class ArbeidstakerutbetalingMelding(
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String,
    val søker: Søker,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val arbeidsgivere: List<ArbeidsgiverDetaljer>,
    val bekreftelser: Bekreftelser,
    val fosterbarn: List<FosterBarn>? = listOf(),
    val titler: List<String>,
    val vedleggUrls: List<URI>
)


data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    val erEØSLand: JaNei
)
typealias Opphold = Bosted

data class ArbeidsgiverDetaljer(
    val navn: String? = null,
    val organisasjonsnummer: String? = null,
    val harHattFraværHosArbeidsgiver: Boolean,
    val arbeidsgiverHarUtbetaltLønn: Boolean,
    val ansettelseslengde: Ansettelseslengde,
    val perioder: List<Utbetalingsperiode>
)

data class OrganisasjonDetaljer(
    val navn: String? = null,
    val organisasjonsnummer: String,
    val harHattFraværHosArbeidsgiver: Boolean,
    val arbeidsgiverHarUtbetaltLønn: Boolean,
    val perioder: List<Utbetalingsperiode>
)

data class Bekreftelser(
    val harBekreftetOpplysninger: JaNei,
    val harForståttRettigheterOgPlikter: JaNei
)

data class Utbetalingsperiode(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val lengde: Duration? = null
)

data class Søker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate?,
    val aktørId: String
) {
    override fun toString(): String {
        return "Soker(fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn', fødselsdato=$fødselsdato, aktørId='$aktørId')"
    }
}

data class FosterBarn(
    val fødselsnummer: String
)

data class SpørsmålOgSvar(
    val spørsmål: Spørsmål,
    val svar: JaNei
)

/**
 * Unngå `Boolean` default-verdi null -> false
 */
enum class JaNei (@get:JsonValue val boolean: Boolean) {
    Ja(true),
    Nei(false);

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraBoolean(boolean: Boolean?) = when(boolean) {
            true -> Ja
            false -> Nei
            else -> throw IllegalStateException("Kan ikke være null")
        }
    }
}

typealias Spørsmål = String
