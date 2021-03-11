package no.nav.helse.dokument

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.søknad.Søknad
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding

class Søknadsformat {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)

        internal fun somJson(
            melding: Søknad
        ): ByteArray {
            val node = objectMapper.valueToTree<ObjectNode>(melding)
            return objectMapper.writeValueAsBytes(node)
        }
    }
}
