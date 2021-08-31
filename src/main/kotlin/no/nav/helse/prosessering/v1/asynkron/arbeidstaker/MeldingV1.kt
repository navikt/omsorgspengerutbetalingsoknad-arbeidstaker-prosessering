package no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.Ansettelseslengde
import no.nav.k9.søknad.Søknad
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
    val fosterbarn: List<FosterBarn>? = listOf(), // TODO: 31/08/2021 Fjerner hele feltet når frontend er prodsatt
    val andreUtbetalinger: List<String>? = listOf(), // TODO: 31/08/2021 Fjerner hele feltet når frontend er prodsatt
    val erSelvstendig: Boolean = false,
    val erFrilanser: Boolean = false,
    val titler: List<String>,
    val vedleggUrls: List<URI>,
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
    val navn: String? = null,
    val organisasjonsnummer: String? = null,
    val harHattFraværHosArbeidsgiver: Boolean,
    val arbeidsgiverHarUtbetaltLønn: Boolean,
    val ansettelseslengde: Ansettelseslengde? = null, // TODO: 31/08/2021 Fjerne hele feltet når frontend er prodsatt
    val perioder: List<Utbetalingsperiode>,
    val utbetalingsårsak: Utbetalingsårsak? = null, // TODO: 31/08/2021 Fjerne nullable når frontend er prodsatt
    val konfliktForklaring: String? = null
) {
    override fun toString(): String {
        return "ArbeidsgiverDetaljer()"
    }
}

enum class Utbetalingsårsak(){
    ARBEIDSGIVER_KONKURS,
    NYOPPSTARTET_HOS_ARBEIDSGIVER,
    KONFLIKT_MED_ARBEIDSGIVER
}

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
    val antallTimerBorte: Duration? = null,
    val antallTimerPlanlagt: Duration? = null,
    val årsak: FraværÅrsak? = null //Fjerner null og optinal når feltet er prodsatt i frontend og api
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

data class FosterBarn(
    @JsonAlias("fødselsnummer")
    val identitetsnummer: String
) {
    override fun toString(): String {
        return "FosterBarn()"
    }
}

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