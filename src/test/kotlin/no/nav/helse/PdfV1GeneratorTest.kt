package no.nav.helse

import no.nav.helse.prosessering.v1.PdfV1Generator
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
            melding = SøknadUtils.defaultSøknad.copy(søknadId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    //@Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
