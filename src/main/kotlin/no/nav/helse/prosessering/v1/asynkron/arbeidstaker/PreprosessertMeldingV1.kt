package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.aktoer.AktørId
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.*
import java.net.URI
import java.time.ZonedDateTime

data class PreprosessertArbeidstakerutbetalingMelding(
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String?,
    val søker: PreprossesertSøker,
    val arbeidsgivere: List<ArbeidsgiverDetaljer>,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val fosterbarn: List<FosterBarn>? = listOf(),
    val bekreftelser: Bekreftelser,
    val dokumentUrls: List<List<URI>>,
    val titler: List<String>,
    val hjemmePgaSmittevernhensyn: Boolean? = null, //TODO 15.03.2021 - Fjernes når frontend er prodsatt
    val hjemmePgaStengtBhgSkole: Boolean? = null, //TODO 15.03.2021 - Fjernes når frontend er prodsatt
    val barn: List<Barn>
) {
    internal constructor(
        melding: ArbeidstakerutbetalingMelding,
        dokumentUrls: List<List<URI>>,
        søkerAktørId: AktørId
    ) : this(
        soknadId = melding.søknadId,
        mottatt = melding.mottatt,
        språk = melding.språk,
        søker = PreprossesertSøker(melding.søker, søkerAktørId),
        arbeidsgivere = melding.arbeidsgivere,
        bosteder = melding.bosteder,
        opphold = melding.opphold,
        fosterbarn = melding.fosterbarn,
        bekreftelser = melding.bekreftelser,
        dokumentUrls = dokumentUrls,
        titler = melding.titler,
        hjemmePgaSmittevernhensyn = melding.hjemmePgaSmittevernhensyn,
        hjemmePgaStengtBhgSkole = melding.hjemmePgaStengtBhgSkole,
        barn = melding.barn
    )

    override fun toString(): String {
        return "PreprosessertArbeidstakerutbetalingMelding(soknadId='$soknadId', mottatt=$mottatt)"
    }

}

data class PreprossesertSøker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktørId: String
) {
    internal constructor(søker: Søker, aktørId: AktørId) : this(
        fødselsnummer = søker.fødselsnummer,
        fornavn = søker.fornavn,
        mellomnavn = søker.mellomnavn,
        etternavn = søker.etternavn,
        aktørId = aktørId.id
    )

    override fun toString(): String {
        return "PreprossesertSøker()"
    }

}
