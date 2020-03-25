package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface OverstyrUttakValideringFeil extends DeklarerteFeil {

    @TekniskFeil(feilkode = "FP-431111", feilmelding = "Uttaksperiode mangler begrunnelse", logLevel = WARN)
    Feil periodeManglerBegrunnelse();

    @TekniskFeil(feilkode = "FP-717234", feilmelding = "Uttaksperiode mangler resultat", logLevel = WARN)
    Feil periodeManglerResultat();

    @TekniskFeil(feilkode = "FP-138943", feilmelding = "Saksbehandler må sette utbetalingsgrad for alle arbeidsforhold for alle perioder som gikk til manuell behandling. Mangler for periode %s", logLevel = WARN)
    Feil manglerUtbetalingsgrad(LocalDateInterval periode);

    @TekniskFeil(feilkode = "FP-634871", feilmelding = "Ugyldig splitting av periode", logLevel = WARN)
    Feil ugyldigSplittingAvPeriode();

    @TekniskFeil(feilkode = "FP-128621", feilmelding = "Trekkdager for periodene overskrider maks tilgjengelige dager på konto", logLevel = WARN)
    Feil trekkdagerOverskriderKontoMaksDager();

    @TekniskFeil(feilkode = "FP-999187", feilmelding = "Perioder før endringsdato kan ikke endres", logLevel = WARN)
    Feil perioderFørEndringsdatoKanIkkeEndres();
}
