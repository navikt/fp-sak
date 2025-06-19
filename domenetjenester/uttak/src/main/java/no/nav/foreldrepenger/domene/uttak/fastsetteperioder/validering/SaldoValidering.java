package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;

public class SaldoValidering implements OverstyrUttakPerioderValidering {

    private static final Logger LOG = LoggerFactory.getLogger(SaldoValidering.class);

    private final SaldoUtregning saldoUtregning;
    private final boolean harAnnenpart;
    private final boolean tapendeBehandling;


    public SaldoValidering(SaldoUtregning saldoUtregning,
                           boolean harAnnenpart,
                           boolean tapendeBehandling) {
        this.saldoUtregning = saldoUtregning;
        this.harAnnenpart = harAnnenpart;
        this.tapendeBehandling = tapendeBehandling;
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
        if (saldoUtregning.getMaxDagerFlerbarnsdager().merEnn0()) {
            var restSaldoDagerFlerbarnsdager = saldoUtregning.restSaldoFlerbarnsdager();
            if (round(restSaldoDagerFlerbarnsdager) < 0) {
                throw OverstyrUttakValideringFeil.trekkdagerOverskriderKontoMaksDager();
            }
        }
        if (saldoUtregning.getMaxDagerUtenAktivitetskrav().merEnn0()) {
            var restSaldoDagerUtenAktivitetskrav = saldoUtregning.restSaldoDagerUtenAktivitetskrav();
            if (round(restSaldoDagerUtenAktivitetskrav) < 0) {
                throw OverstyrUttakValideringFeil.trekkdagerOverskriderKontoMaksDager();
            }
        }
        if (saldoUtregning.getMaxDagerMinsterett().merEnn0()) {
            var restSaldoDagerMinsterett = saldoUtregning.restSaldoMinsterett();
            if (round(restSaldoDagerMinsterett) < 0) {
                throw OverstyrUttakValideringFeil.trekkdagerOverskriderKontoMaksDager();
            }
        }
    }

    public SaldoValideringResultat valider(Stønadskontotype stønadskontoType) {
        if (tapendeBehandling || !harAnnenpart) {
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

    public static int round(Trekkdager trekkdager) {
        //Ved gradering kan saldo med desimal være feks -0.2. Dette skal tolkes som 0. Man skal ikke bikke over til -1 så lenge trekkdager er mellom -1 og 0
        return trekkdager.mindreEnn0() ? trekkdager.decimalValue()
            .setScale(0, RoundingMode.DOWN)
            .intValue() : trekkdager.rundOpp();
    }

    public record SaldoValideringResultat(boolean isGyldig, boolean isNegativPgaSamtidigUttak) {
    }
}
