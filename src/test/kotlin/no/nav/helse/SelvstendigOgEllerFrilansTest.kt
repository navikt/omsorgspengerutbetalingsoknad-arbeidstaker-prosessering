package no.nav.helse

import no.nav.helse.prosessering.v1.formattedSelvstendigOgEllerFrilans
import kotlin.test.Test
import kotlin.test.assertEquals

class SelvstendigOgEllerFrilansTest {
    @Test
    fun `Håndterer kun gyldige string verdier og formatterer korrekt`() {

        val input1: List<String> = listOf()
        val input2: List<String> = listOf("ugyldigVerdi")
        val input3: List<String> = listOf("ugyldigVerdi", "selvstendig", "frilans", "enAnnenUgyldigVerdi")

        assertEquals(listOf(), input1.formattedSelvstendigOgEllerFrilans())
        assertEquals(listOf(), input2.formattedSelvstendigOgEllerFrilans())
        assertEquals(listOf("Ja, er selvstendig næringsdrivende", "Ja, er frilanser"), input3.formattedSelvstendigOgEllerFrilans())
    }
}
