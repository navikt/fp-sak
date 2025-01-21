package no.nav.foreldrepenger.domene.fpinntektsmelding;

public record SendNyBeskjedResponse(NyBeskjedResultat nyBeskjedResultat) {
    public enum NyBeskjedResultat {
        FORESPØRSEL_FINNES_IKKE,
        NY_BESKJED_SENDT
    }
}
