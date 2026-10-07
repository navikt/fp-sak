package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

/**
 * MIDLERTIDIG. Resultat av {@link ForespørselStatusVurderingTjeneste#vurder}, brukt av en engangs ryddejobb i
 * fp-inntektsmelding for å avgjøre hva som skal gjøres med en forespørsel.
 * <p>
 * Et viktig skille: {@link #SETT_TIL_UTGÅTT} skal kun brukes der det ikke lenger er aktuelt å motta inntektsmelding
 * for kombinasjonen (saken er avsluttet, eller det er ingen påkrevde inntektsmeldinger for orgnummeret).
 * {@link #SETT_TIL_FERDIG} brukes der saken fortsatt er løpende og har påkrevde inntektsmeldinger, men der alle
 * allerede er mottatt. Forskjellen er viktig: en forespørsel satt til utgått hindrer arbeidsgiver fra å sende inn
 * en (ny) inntektsmelding, mens ferdig fortsatt tillater dette så lenge saken er løpende.
 */
public enum ForespørselVurderingResultat {
    TRENGER_FORTSATT_INNTEKTSMELDING,
    SETT_TIL_UTGÅTT,
    SETT_TIL_FERDIG
}
