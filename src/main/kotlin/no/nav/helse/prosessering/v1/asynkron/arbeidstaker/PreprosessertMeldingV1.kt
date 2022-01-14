package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.k9.søknad.Søknad
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.*
import java.time.ZonedDateTime

data class PreprosessertMelding(
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String?,
    val søker: Søker,
    val arbeidsgivere: List<ArbeidsgiverDetaljer>,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val bekreftelser: Bekreftelser,
    val dokumentId: List<List<String>>,
    val titler: List<String>,
    val hjemmePgaSmittevernhensyn: Boolean,
    val hjemmePgaStengtBhgSkole: Boolean? = null,
    val k9Format: Søknad
) {
    internal constructor(
        melding: MeldingV1,
        dokumentId: List<List<String>>,
    ) : this(
        soknadId = melding.søknadId,
        mottatt = melding.mottatt,
        språk = melding.språk,
        søker = melding.søker,
        arbeidsgivere = melding.arbeidsgivere,
        bosteder = melding.bosteder,
        opphold = melding.opphold,
        bekreftelser = melding.bekreftelser,
        dokumentId = dokumentId,
        titler = melding.titler,
        hjemmePgaSmittevernhensyn = melding.hjemmePgaSmittevernhensyn,
        hjemmePgaStengtBhgSkole = melding.hjemmePgaStengtBhgSkole,
        k9Format = melding.k9Format
    )

    override fun toString(): String {
        return "PreprosessertMelding(soknadId='$soknadId', mottatt=$mottatt)"
    }
}