package no.nav.helse

import no.nav.helse.prosessering.DAGER_SYNLIG_K9BESKJED
import no.nav.helse.prosessering.TEKST_K9BESKJED
import no.nav.helse.prosessering.YTELSE_K9BESKJED
import no.nav.k9.søknad.JsonUtils
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
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker

internal object SøknadUtils {
    private val start = LocalDate.parse("2020-01-01")
    private const val GYLDIG_ORGNR = "917755736"

    internal val defaultSøknad = MeldingV1(
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
                utbetalingsårsak = Utbetalingsårsak.NYOPPSTARTET_HOS_ARBEIDSGIVER,
                årsakNyoppstartet = ÅrsakNyoppstartet.UTØVDE_VERNEPLIKT,
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
                årsakNyoppstartet = ÅrsakNyoppstartet.SØKTE_ANDRE_UTBETALINGER,
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
        vedleggId = listOf("1234", "5678"),
        hjemmePgaSmittevernhensyn = true,
        hjemmePgaStengtBhgSkole = true,
        k9Format = Søknad(
            SøknadId(UUID.randomUUID().toString()),
            Versjon("1.0.0"),
            ZonedDateTime.now(),
            K9Søker(NorskIdentitetsnummer.of("02119970078")),
            OmsorgspengerUtbetaling(
                listOf(
                    Barn().medNorskIdentitetsnummer(NorskIdentitetsnummer.of("26128027024"))
                ),
                null,
                listOf(
                    FraværPeriode(
                        Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")),
                        Duration.ofHours(7).plusMinutes(30),
                        null,
                        no.nav.k9.søknad.felles.fravær.FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE,
                        SøknadÅrsak.KONFLIKT_MED_ARBEIDSGIVER,
                        listOf(AktivitetFravær.ARBEIDSTAKER),
                        Organisasjonsnummer.of(GYLDIG_ORGNR),
                        null
                    )
                ),
                null,
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

internal fun String.assertK9Beskjed(søknad: MeldingV1){
    val k9Beskjed = JSONObject(this)

    assertEquals(søknad.søknadId, k9Beskjed.getString("grupperingsId"))
    assertEquals(DAGER_SYNLIG_K9BESKJED, k9Beskjed.getLong("dagerSynlig"))
    assertEquals(TEKST_K9BESKJED, k9Beskjed.getString("tekst"))
    assertEquals(YTELSE_K9BESKJED, k9Beskjed.getString("ytelse"))
}

internal fun String.assertCleanupFormat() {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))

    assertNotNull(data.getJSONObject("journalfort")).getString("journalpostId")
    val søknad = assertNotNull(data.getJSONObject("melding")).getJSONObject("k9Format")

    val rekonstruertSøknad = JsonUtils.getObjectMapper().readValue(søknad.toString(), Søknad::class.java)
    JSONAssert.assertEquals(søknad.toString(), JsonUtils.toString(rekonstruertSøknad), true)
}