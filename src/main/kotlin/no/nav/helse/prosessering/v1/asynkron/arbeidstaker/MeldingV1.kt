package no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.k9.søknad.Søknad
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1(
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String,
    val søker: Søker,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val arbeidsgivere: List<ArbeidsgiverDetaljer>,
    val bekreftelser: Bekreftelser,
    val titler: List<String>,
    val vedleggUrls: List<URI> = listOf(),
    val vedleggId: List<String> = listOf(),
    val hjemmePgaSmittevernhensyn: Boolean,
    val hjemmePgaStengtBhgSkole: Boolean? = null,
    val k9Format: Søknad
) {
    override fun toString(): String {
        return "ArbeidstakerutbetalingMelding(søknadId='$søknadId', mottatt=$mottatt)"
    }
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    val erEØSLand: JaNei
)
typealias Opphold = Bosted

data class ArbeidsgiverDetaljer(
    val navn: String,
    val organisasjonsnummer: String,
    val harHattFraværHosArbeidsgiver: Boolean,
    val arbeidsgiverHarUtbetaltLønn: Boolean,
    val perioder: List<Utbetalingsperiode>,
    val utbetalingsårsak: Utbetalingsårsak,
    val konfliktForklaring: String? = null,
    val årsakNyoppstartet: ÅrsakNyoppstartet? = null
) {
    override fun toString(): String {
        return "ArbeidsgiverDetaljer()"
    }
}

enum class ÅrsakNyoppstartet(val pdfTekst: String){
    JOBBET_HOS_ANNEN_ARBEIDSGIVER("Jeg jobbet for en annen arbeidsgiver."),
    VAR_FRILANSER("Jeg var frilanser."),
    VAR_SELVSTENDIGE("Jeg var selvstendig næringsdrivende."),
    SØKTE_ANDRE_UTBETALINGER("Jeg søkte om eller mottok dagpenger, foreldrepenger, svangerskapspenger, sykepenger, omsorgspenger, pleiepenger, opplæringspenger, kompensasjonsytelse for selvstendig næringsdrivende eller frilanser."),
    ARBEID_I_UTLANDET("Jeg jobbet i utlandet som arbeidstaker, selvstendig næringsdrivende eller frilanser."),
    UTØVDE_VERNEPLIKT("Jeg utøvde verneplikt."),
    ANNET("Annet.")
}

enum class Utbetalingsårsak(val pdfTekst: String){
    ARBEIDSGIVER_KONKURS("Arbeidsgiver er konkurs."),
    NYOPPSTARTET_HOS_ARBEIDSGIVER("Jeg har jobbet mindre enn 4 uker hos denne arbeidsgiveren."),
    KONFLIKT_MED_ARBEIDSGIVER("Uenighet med arbeidsgiver.")
}

data class Bekreftelser(
    val harBekreftetOpplysninger: JaNei,
    val harForståttRettigheterOgPlikter: JaNei
)

data class Utbetalingsperiode(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val antallTimerBorte: Duration? = null,
    val antallTimerPlanlagt: Duration? = null,
    val årsak: FraværÅrsak
)

enum class FraværÅrsak {
    STENGT_SKOLE_ELLER_BARNEHAGE,
    SMITTEVERNHENSYN,
    ORDINÆRT_FRAVÆR
}

data class Søker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate?,
    val aktørId: String
) {
    override fun toString(): String {
        return "Soker(fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn', fødselsdato=$fødselsdato, aktørId='******')"
    }
}

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