package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;

public class SaldoValidering implements OverstyrUttakPerioderValidering {

    private static final Logger LOG = LoggerFactory.getLogger(SaldoValidering.class);

    private final SaldoUtregning saldoUtregning;
    private final boolean harAnnenpart;
    private final boolean berørtBehandling;


    public SaldoValidering(SaldoUtregning saldoUtregning,
                           boolean harAnnenpart,
                           boolean berørtBehandling) {
        this.saldoUtregning = saldoUtregning;
        this.harAnnenpart = harAnnenpart;
        this.berørtBehandling = berørtBehandling;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var stønadskontoType : saldoUtregning.stønadskontoer()) {
            var valideringResultat = valider(stønadskontoType);
            if (!valideringResultat.isGyldig()) {
                throw OverstyrUttakValideringFeil.trekkdagerOverskriderKontoMaksDager();
            }
            if (valideringResultat.isNegativPgaSamtidigUttak()) {
                LOG.info("Saksbehandler går videre med negativ saldo pga samtidig uttak");
            }
        }
        if (saldoUtregning.getMaxDagerUtenAktivitetskrav().merEnn0()) {
            var restSaldoDagerUtenAktivitetskrav = saldoUtregning.restSaldoDagerUtenAktivitetskrav();
            if (restSaldoDagerUtenAktivitetskrav.mindreEnn0()) {
                throw OverstyrUttakValideringFeil.trekkdagerOverskriderKontoMaksDager();
            }
        }
        if (saldoUtregning.getMaxDagerMinsterett().merEnn0()) {
            var restSaldoDagerMinsterett = saldoUtregning.restSaldoMinsterett();
            if (restSaldoDagerMinsterett.mindreEnn0()) {
                throw OverstyrUttakValideringFeil.trekkdagerOverskriderKontoMaksDager();
            }
        }
    }

    public SaldoValideringResultat valider(Stønadskontotype stønadskontoType) {
        if (berørtBehandling || !harAnnenpart) {
            var isGyldig = !saldoUtregning.negativSaldo(stønadskontoType);
            return new SaldoValideringResultat(isGyldig, false);
        }
        if (saldoUtregning.negativSaldo(stønadskontoType)) {
            if (saldoUtregning.nokDagerÅFrigiPåAnnenpart(stønadskontoType)) {
                return new SaldoValideringResultat(true, false);
            }
            if (saldoUtregning.søktSamtidigUttak(stønadskontoType)) {
                return new SaldoValideringResultat(true, true);
            }
            return new SaldoValideringResultat(false, false);
        }
        return new SaldoValideringResultat(true, false);
    }

    public static record SaldoValideringResultat(boolean isGyldig, boolean isNegativPgaSamtidigUttak) {
    }
}
