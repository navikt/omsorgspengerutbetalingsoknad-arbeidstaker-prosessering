package no.nav.helse

import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.arbeidstaker.tilK9RapidBehovssekvens
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.Barn
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZonedDateTime

class AleneOmOmsorgenFormatTest {

    @Test
    fun `Gyldig melding blir til forventet AleneOmOmsorgen Behovssekvens, hvor kun barnmed aleneomsorg blir registrert`(){
        val ulid = "01F12XM8QSH9WZMM9Q7MT071DF"
        val mottatt = ZonedDateTime.parse("2021-01-01T16:24:11.252+05:30[Asia/Calcutta]")
        val gyldigSøknad = SøknadUtils.defaultSøknad.copy(
            mottatt = mottatt,
            barn = listOf(
                Barn(
                    navn = "Ole Dole",
                    identitetsnummer = "14128622870",
                    aktørId = "12345",
                    aleneOmOmsorgen = true
                ),
                Barn(
                    navn = "Doffen",
                    identitetsnummer = "07097427806",
                    aktørId = "67899",
                    aleneOmOmsorgen = true
                ),
                Barn(
                    navn = "Anton",
                    identitetsnummer = "07097427806",
                    aktørId = "67899",
                    aleneOmOmsorgen = false
                )

            )
        )

        val (id, løsning) = gyldigSøknad.tilK9RapidBehovssekvens(Metadata(1,"11111111111", "123"), ulid).keyValue
        val behovssekvensLøsningSomJson = JSONObject(løsning)
        behovssekvensLøsningSomJson.remove("@opprettet")
        behovssekvensLøsningSomJson.remove("@sistEndret")

        val forventet = """
            {
              "@id": "01F12XM8QSH9WZMM9Q7MT071DF",
              "@type": "Behovssekvens",
              "@versjon": "1",
              "@correlationId": "11111111111",
              "@behovsrekkefølge": [
                "AleneOmOmsorgen"
              ],
              "@behov": {
                "AleneOmOmsorgen": {
                  "versjon": "1.0.0",
                  "identitetsnummer": "02119970078",
                  "mottaksdato": "2021-01-01",
                  "barn": [
                    {
                      "identitetsnummer": "14128622870",
                      "fødselsdato": "2021-01-01"
                    },
                    {
                      "identitetsnummer": "07097427806",
                      "fødselsdato": "2021-01-01"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, behovssekvensLøsningSomJson, true)
    }

}