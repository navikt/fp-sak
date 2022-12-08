package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

final class OverstyrUttakValideringFeil {

    private OverstyrUttakValideringFeil() {
    }

    static TekniskException periodeManglerBegrunnelse() {
        return new TekniskException("FP-431111", "Uttaksperiode mangler begrunnelse");
    }

    static TekniskException periodeManglerResultat() {
        return new TekniskException("FP-717234", "Uttaksperiode mangler resultat");
    }

    static TekniskException manglerUtbetalingsgrad(LocalDateInterval periode) {
        var msg = String.format("""
            Saksbehandler må sette utbetalingsgrad for alle arbeidsforhold for alle perioder
            som gikk til manuell behandling. Mangler for periode %s
            """, periode);
        return new TekniskException("FP-138943", msg);
    }

    static TekniskException ugyldigSplittingAvPeriode() {
        return new TekniskException("FP-634871", "Ugyldig splitting av periode");
    }

    static TekniskException trekkdagerOverskriderKontoMaksDager() {
        return new TekniskException("FP-128621",
            "Trekkdager for periodene overskrider maks tilgjengelige dager på konto");
    }

    static TekniskException perioderFørEndringsdatoKanIkkeEndres(LocalDate endringsdato, ForeldrepengerUttakPeriode periode) {
        return new TekniskException("FP-999187", "Perioder før endringsdato " + endringsdato + " kan ikke endres " + periode);
    }

    static TekniskException trekkerDagerFraSøktKontoVedAvslagAvOverføring(ForeldrepengerUttakPeriode periode) {
        var msg = String.format("""
            Kan ikke trekke dager fra søkt kvote ved avslag av overføring. Dette gjelder periode %s - %s
            """, periode.getFom(), periode.getTom());
        return new TekniskException("FP-135672", msg);
    }
}
