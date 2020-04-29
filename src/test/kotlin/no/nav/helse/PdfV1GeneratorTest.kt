package no.nav.helse

import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.JobbHosNåværendeArbeidsgiver
import org.junit.Ignore
import java.io.File
import java.time.LocalDate
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val fødselsdato = LocalDate.now()

        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad-arbeidstaker"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultArbeidstakerutbetalingMelding.copy(søknadId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "2-full-søknad-arbeidstaker-annet-arbeidsforhold"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultArbeidstakerutbetalingMelding.copy(
                søknadId = id,
                jobbHosNåværendeArbeidsgiver = JobbHosNåværendeArbeidsgiver(
                    merEnn4Uker = false,
                    begrunnelse = JobbHosNåværendeArbeidsgiver.Begrunnelse.ANNET_ARBEIDSFORHOLD
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "3-full-søknad-arbeidstaker-andre-ytelser"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultArbeidstakerutbetalingMelding.copy(
                søknadId = id,
                jobbHosNåværendeArbeidsgiver = JobbHosNåværendeArbeidsgiver(
                    merEnn4Uker = false,
                    begrunnelse = JobbHosNåværendeArbeidsgiver.Begrunnelse.ANDRE_YTELSER
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "4-full-søknad-arbeidstaker-lovbestemt-ferie-eller-ulønnet-permisjon"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultArbeidstakerutbetalingMelding.copy(
                søknadId = id,
                jobbHosNåværendeArbeidsgiver = JobbHosNåværendeArbeidsgiver(
                    merEnn4Uker = false,
                    begrunnelse = JobbHosNåværendeArbeidsgiver.Begrunnelse.LOVBESTEMT_FERIE_ELLER_ULØNNET_PERMISJON
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "5-full-søknad-arbeidstaker-militærtjeneste"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultArbeidstakerutbetalingMelding.copy(
                søknadId = id,
                jobbHosNåværendeArbeidsgiver = JobbHosNåværendeArbeidsgiver(
                    merEnn4Uker = false,
                    begrunnelse = JobbHosNåværendeArbeidsgiver.Begrunnelse.MILITÆRTJENESTE
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    @Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
