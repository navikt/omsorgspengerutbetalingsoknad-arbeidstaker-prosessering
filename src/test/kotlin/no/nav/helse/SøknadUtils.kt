package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertArbeidstakerutbetalingMelding
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.fravær.SøknadÅrsak
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.*
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.*
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker

internal object SøknadUtils {
    internal val objectMapper = jacksonObjectMapper().omsorgspengerKonfiguert()
    private val start = LocalDate.parse("2020-01-01")
    private const val GYLDIG_ORGNR = "917755736"

    internal val defaultSøknad = ArbeidstakerutbetalingMelding(
        søknadId = UUID.randomUUID().toString(),
        språk = "nb",
        mottatt = ZonedDateTime.now(),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "02119970078",
            fødselsdato = LocalDate.parse("1999-11-02"),
            etternavn = "Nordmann",
            mellomnavn = null,
            fornavn = "Ola"
        ),
        arbeidsgivere = listOf(
            ArbeidsgiverDetaljer(
                navn = "Arbeidsgiver 1",
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start,
                        tilOgMed = start.plusDays(10),
                        antallTimerPlanlagt = Duration.ofHours(8),
                        antallTimerBorte = Duration.ofHours(8),
                        årsak = FraværÅrsak.SMITTEVERNHENSYN
                    )
                ),
                utbetalingsårsak = Utbetalingsårsak.KONFLIKT_MED_ARBEIDSGIVER,
                konfliktForklaring = "Har en konflikt med arbeidsgiver fordi ...."
            ),
            ArbeidsgiverDetaljer(
                navn = "Arbeidsgiver 2",
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                utbetalingsårsak = Utbetalingsårsak.ARBEIDSGIVER_KONKURS,
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusDays(20),
                        tilOgMed = start.plusDays(20),
                        antallTimerPlanlagt = Duration.ofHours(8),
                        antallTimerBorte = Duration.ofHours(8),
                        årsak = FraværÅrsak.ORDINÆRT_FRAVÆR
                    )
                )
            ),
            ArbeidsgiverDetaljer(
                navn = "Arbeidsgiver 3",
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                utbetalingsårsak = Utbetalingsårsak.NYOPPSTARTET_HOS_ARBEIDSGIVER,
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusMonths(1),
                        tilOgMed = start.plusMonths(1).plusDays(5),
                        årsak = FraværÅrsak.ORDINÆRT_FRAVÆR
                    )
                )
            )
        ),
        bosteder = listOf(
            Bosted(
                fraOgMed = start.minusDays(20),
                tilOgMed = start.minusDays(10),
                landkode = "GB",
                landnavn = "Great Britain",
                erEØSLand = JaNei.Ja
            ),
            Bosted(
                fraOgMed = start.minusDays(20),
                tilOgMed = start.minusDays(10),
                landkode = "US",
                landnavn = "USA",
                erEØSLand = JaNei.Nei
            )
        ),
        opphold = listOf(
            Opphold(
                fraOgMed = start.minusDays(20),
                tilOgMed = start.minusDays(10),
                landkode = "GB",
                landnavn = "Great Britain",
                erEØSLand = JaNei.Ja
            )
        ),
        bekreftelser = Bekreftelser(
            harForståttRettigheterOgPlikter = JaNei.Ja,
            harBekreftetOpplysninger = JaNei.Ja
        ),
        titler = listOf(
            "vedlegg1"
        ),
        vedleggUrls = listOf(
            URI("http://localhost:8080/vedlegg/1"),
            URI("http://localhost:8080/vedlegg/2"),
            URI("http://localhost:8080/vedlegg/3")
        ),
        hjemmePgaSmittevernhensyn = true,
        hjemmePgaStengtBhgSkole = true,
        k9Format = Søknad(
            SøknadId(UUID.randomUUID().toString()),
            Versjon("1.0.0"),
            ZonedDateTime.now(),
            K9Søker(NorskIdentitetsnummer.of("02119970078")),
            OmsorgspengerUtbetaling(
                listOf(
                    Barn(NorskIdentitetsnummer.of("26128027024"), null)
                ),
                null,
                listOf(
                    FraværPeriode(
                        Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")),
                        Duration.ofHours(7).plusMinutes(30),
                        no.nav.k9.søknad.felles.fravær.FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE,
                        SøknadÅrsak.KONFLIKT_MED_ARBEIDSGIVER,
                        listOf(AktivitetFravær.ARBEIDSTAKER),
                        Organisasjonsnummer.of(GYLDIG_ORGNR)
                    )
                ),
                Bosteder().medPerioder(
                    mapOf(
                        Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")) to
                                Bosteder.BostedPeriodeInfo().medLand((Landkode.NORGE))
                    )
                ),
                Utenlandsopphold().medPerioder(
                    mapOf(
                        Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")) to
                                Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
                                    .medLand(Landkode.SPANIA)
                                    .medÅrsak(Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
                    )
                )
            )
        )
    )
}

internal fun ArbeidstakerutbetalingMelding.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
internal fun PreprosessertArbeidstakerutbetalingMelding.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
