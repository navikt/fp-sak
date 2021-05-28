package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import java.time.Period;

/*
 * Opptjeningsperiodens lengde
 * Tidligste oppstart ytelse før fødsel
 */
public record OpptjeningsperiodevilkårParametre(Period periodeLengde,
                                                Period tidligsteUttakFørFødselPeriode) {

    public static OpptjeningsperiodevilkårParametre vilkårparametreForeldrepenger() {
        return new OpptjeningsperiodevilkårParametre(Period.ofMonths(10), Period.ofWeeks(12));
    }

    public static OpptjeningsperiodevilkårParametre vilkårparametreSvangerskapspenger() {
        return new OpptjeningsperiodevilkårParametre(Period.ofDays(28), null);
    }

}
