package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.omsorgspengerKonfiguert
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

internal class PdfV1Generator {
    private companion object {
        private val mapper = jacksonObjectMapper().omsorgspengerKonfiguert()

        private const val ROOT = "handlebars"
        private const val SØKNAD = "soknad"

        private val REGULAR_FONT = "$ROOT/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
        private val BOLD_FONT = "$ROOT/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
        private val ITALIC_FONT = "$ROOT/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()

        private val sRGBColorSpace = "$ROOT/sRGB.icc".fromResources().readBytes()

        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("eq", Helper<String> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("eqJaNei", { context: Boolean, options ->
                val con = when (context) {
                    true -> "Ja"
                    false -> "Nei"
                }
                if (con == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("fritekst", Helper<String> { context, _ ->
                if (context == null) "" else {
                    val text = Handlebars.Utils.escapeExpression(context)
                        .toString()
                        .replace(Regex("\\r\\n|[\\n\\r]"), "<br/>")
                    Handlebars.SafeString(text)
                }
            })
            registerHelper("dato", Helper<String> { context, _ ->
                DATE_FORMATTER.format(LocalDate.parse(context))
            })
            registerHelper("storForbokstav", Helper<String> { context, _ ->
                context.capitalize()
            })
            registerHelper("tidspunkt", Helper<String> { context, _ ->
                DATE_TIME_FORMATTER.format(ZonedDateTime.parse(context))
            })
            registerHelper("varighet", Helper<String> { context, _ ->
                Duration.parse(context).tilString()
            })
            registerHelper("jaNeiSvar", Helper<Boolean> { context, _ ->
                if (context == true) "Ja" else "Nei"
            })
            registerHelper("årsak", Helper<String> { context, _ ->
                when(FraværÅrsak.valueOf(context)) {
                    FraværÅrsak.ORDINÆRT_FRAVÆR -> "Ordinært fravær"
                    FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE -> "Stengt skole eller barnehage"
                    FraværÅrsak.SMITTEVERNHENSYN -> "Smittevernhensyn"
                }
            })

            infiniteLoops(true)
        }

        private val soknadTemplateLoader = handlebars.compile(SØKNAD)

        private val ZONE_ID = ZoneId.of("Europe/Oslo")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)
    }

    internal fun generateSoknadOppsummeringPdf(
        melding: MeldingV1
    ): ByteArray {
        XRLog.listRegisteredLoggers().forEach { logger -> XRLog.setLevel(logger, Level.WARNING) }
        val mottatt = melding.mottatt.toLocalDate()
        soknadTemplateLoader.apply(
            Context
                .newBuilder(
                    mapOf(
                        "søknad" to melding.somMap(),
                        "språk" to melding.språk.sprakTilTekst(),
                        "mottaksUkedag" to melding.mottatt.withZoneSameInstant(ZONE_ID).norskDag(),
                        "søker" to mapOf(
                            "formatertNavn" to melding.søker.formatertNavn().capitalizeName()
                        ),
                        "medlemskap" to mapOf(
                            "siste12" to melding.bosteder.any {
                                it.fraOgMed.isBefore(mottatt) || it.tilOgMed.isEqual(mottatt)
                            },
                            "neste12" to melding.bosteder.any {
                                it.fraOgMed.isEqual(mottatt) || it.fraOgMed.isAfter(mottatt)
                            }
                        ),
                        "harArbeidsgivere" to melding.arbeidsgivere.isNotEmpty(),
                        "arbeidsgivere" to melding.arbeidsgivere.somMap(),
                        "harOpphold" to melding.opphold.isNotEmpty(),
                        "harBosteder" to melding.bosteder.isNotEmpty(),
                        "harVedlegg" to melding.vedleggId.isNotEmpty(),
                        "ikkeHarSendtInnVedlegg" to melding.vedleggId.isEmpty(),
                        "bekreftelser" to melding.bekreftelser.bekreftelserSomMap(),
                        "titler" to mapOf(
                            "vedlegg" to melding.titler.somMapTitler()
                        )
                    )
                )
                .resolver(MapValueResolver.INSTANCE)
                .build()
        ).let { html ->
            val outputStream = ByteArrayOutputStream()

            PdfRendererBuilder()
                .useFastMode()
                .usePdfUaAccessbility(true)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
                .withHtmlContent(html, "")
                .medFonter()
                .useColorProfile(sRGBColorSpace)
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()

            return outputStream.use {
                it.toByteArray()
            }
        }
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont(
            { ByteArrayInputStream(REGULAR_FONT) },
            "Source Sans Pro",
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            false
        )
            .useFont(
                { ByteArrayInputStream(BOLD_FONT) },
                "Source Sans Pro",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                false
            )
            .useFont(
                { ByteArrayInputStream(ITALIC_FONT) },
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.ITALIC,
                false
            )

    private fun MeldingV1.somMap() = mapper.convertValue(
        this,
        object :
            TypeReference<MutableMap<String, Any?>>() {}
    )

}

private fun Bekreftelser.bekreftelserSomMap(): Map<String, Boolean> {
    return mapOf(
        "harBekreftetOpplysninger" to harBekreftetOpplysninger.boolean,
        "harForståttRettigheterOgPlikter" to harForståttRettigheterOgPlikter.boolean
    )
}

private fun Duration.tilString(): String = when (this.toMinutesPart()) {
    0 -> "${this.toHours()} timer"
    else -> "${this.toHoursPart()} timer og ${this.toMinutesPart()} minutter"
}

private fun List<String>.somMapTitler(): List<Map<String, Any?>> {
    return map {
        mapOf(
            "tittel" to it
        )
    }
}

private fun List<ArbeidsgiverDetaljer>.somMap(): List<Map<String, Any?>> {
    return map{
        mapOf(
            "navn" to it.navn,
            "organisasjonsnummer" to it.organisasjonsnummer,
            "utbetalingsårsak" to it.utbetalingsårsak.pdfTekst,
            "harSattKonfliktForklaring" to (it.konfliktForklaring != null),
            "konfliktForklaring" to it.konfliktForklaring,
            "harSattÅrsakNyoppstartet" to (it.årsakNyoppstartet != null),
            "årsakNyoppstartet" to it.årsakNyoppstartet?.pdfTekst
        )
    }
}

private fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { it.lowercase().capitalize() }

private fun String.sprakTilTekst() = when (this.lowercase()) {
    "nb" -> "Bokmål"
    "nn" -> "Nynorsk"
    else -> this
}