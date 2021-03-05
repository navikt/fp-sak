package no.nav.foreldrepenger.domene.uttak.fakta;

import java.time.LocalDate;

import no.nav.vedtak.exception.TekniskException;

public final class KontrollerFaktaUttakFeil {

    public static TekniskException dokumentertUtenBegrunnelse() {
        return new TekniskException("FP-823386",
            "Datafeil. Periode er dokumentert uten at saksbehandler har begrunnet dette.");
    }

    public static TekniskException søktGraderingUtenArbeidsgiver(String periodeType, LocalDate fom, LocalDate tom) {
        var msg = String.format("""
            Ikke gyldig søknadsperiode. Periode med gradering for arbeidstaker trenger arbeidsgiver oppgitt. %s %s %s
            """, periodeType, fom, tom);
        return new TekniskException("FP-651234", msg);
    }
}
