package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

data class Ansettelseslengde(
    val merEnn4Uker: Boolean,
    val begrunnelse: Begrunnelse? = null,
    val ingenAvSituasjoneneForklaring: String? = null
) {
    enum class Begrunnelse {
        ANNET_ARBEIDSFORHOLD,
        ANDRE_YTELSER,
        LOVBESTEMT_FERIE_ELLER_ULØNNET_PERMISJON,
        MILITÆRTJENESTE,
        INGEN_AV_SITUASJONENE
    }
}
