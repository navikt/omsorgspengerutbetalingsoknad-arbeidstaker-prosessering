package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.aktoer.AktørId
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.Ansettelseslengde
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.Ansettelseslengde.Begrunnelse.*
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.PreprosessertArbeidstakerutbetalingMelding
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
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
                ansettelseslengde = Ansettelseslengde(
                    merEnn4Uker = true
                ),
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start,
                        tilOgMed = start.plusDays(10),
                        antallTimerPlanlagt = Duration.ofHours(8),
                        antallTimerBorte = Duration.ofHours(8),
                        årsak = FraværÅrsak.SMITTEVERNHENSYN
                    )
                )
            ),
            ArbeidsgiverDetaljer(
                navn = "Arbeidsgiver 2",
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                ansettelseslengde = Ansettelseslengde(
                    merEnn4Uker = false,
                    begrunnelse = ANNET_ARBEIDSFORHOLD
                ),
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusDays(20),
                        tilOgMed = start.plusDays(20),
                        antallTimerPlanlagt = Duration.ofHours(8),
                        antallTimerBorte = Duration.ofHours(8)
                    )
                )
            ),
            ArbeidsgiverDetaljer(
                navn = "Arbeidsgiver 3",
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                ansettelseslengde = Ansettelseslengde(
                    merEnn4Uker = false,
                    begrunnelse = MILITÆRTJENESTE
                ),
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusDays(30),
                        tilOgMed = start.plusDays(35)
                    )
                )
            ),
            ArbeidsgiverDetaljer(
                navn = "Arbeidsgiver 4",
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                ansettelseslengde = Ansettelseslengde(
                    merEnn4Uker = false,
                    begrunnelse = INGEN_AV_SITUASJONENE,
                    ingenAvSituasjoneneForklaring = "Forklarer hvorfor ingen av situasjonene passer."
                ),
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusDays(30),
                        tilOgMed = start.plusDays(35)
                    )
                )
            ),
            ArbeidsgiverDetaljer(
                organisasjonsnummer = GYLDIG_ORGNR,
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                ansettelseslengde = Ansettelseslengde(
                    merEnn4Uker = false,
                    begrunnelse = ANDRE_YTELSER
                ),
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusMonths(1),
                        tilOgMed = start.plusMonths(1).plusDays(5)
                    )
                )
            ),
            ArbeidsgiverDetaljer(
                navn = "Ikke registrert arbeidsgiver",
                harHattFraværHosArbeidsgiver = true,
                arbeidsgiverHarUtbetaltLønn = false,
                ansettelseslengde = Ansettelseslengde(
                    merEnn4Uker = false,
                    begrunnelse = ANDRE_YTELSER
                ),
                perioder = listOf(
                    Utbetalingsperiode(
                        fraOgMed = start.plusMonths(1),
                        tilOgMed = start.plusMonths(1).plusDays(5)
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
        fosterbarn = listOf(
            FosterBarn(
                fødselsnummer = "02119970078"
            )
        ),
        andreUtbetalinger = listOf("dagpenger", "sykepenger"),
        erSelvstendig = true,
        erFrilanser = true,
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
                        listOf(AktivitetFravær.ARBEIDSTAKER)
                    )
                ),
                Bosteder(
                    mapOf(
                        Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")) to
                                Bosteder.BostedPeriodeInfo(Landkode.NORGE)
                    )
                ),
                Utenlandsopphold(
                    mapOf(
                        Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")) to
                                Utenlandsopphold.UtenlandsoppholdPeriodeInfo.builder()
                                    .land(Landkode.SPANIA)
                                    .årsak(Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
                                    .build()
                    )
                )
            )
        )
    )

    internal val defaultKomplettSøknad = PreprosessertArbeidstakerutbetalingMelding(
        søkerAktørId = AktørId("123456"),
        melding = defaultSøknad,
        dokumentUrls = listOf(
            listOf(URI("http://localhost:8080/vedlegg/1"), URI("http://localhost:8080/vedlegg/2")),
            listOf(URI("http://localhost:8080/vedlegg/3"), URI("http://localhost:8080/vedlegg/4"))
        )
    )
}

internal fun ArbeidstakerutbetalingMelding.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
internal fun PreprosessertArbeidstakerutbetalingMelding.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
