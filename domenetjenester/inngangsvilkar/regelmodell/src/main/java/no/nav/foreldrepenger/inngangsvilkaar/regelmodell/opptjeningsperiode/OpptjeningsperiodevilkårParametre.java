package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import java.time.Period;

/*
 * Opptjeningsperiodens lengde
 * Tidligste oppstart ytelse før fødsel
 */
public record OpptjeningsperiodevilkårParametre(Period periodeLengde,
                                                Period morTidligsteUttakFørTerminPeriode,
                                                Period morSenesteUttakFørTerminPeriode,
                                                Period annenTidligsteUttakFørTerminPeriode) {

    public static OpptjeningsperiodevilkårParametre vilkårparametreForeldrepenger(LovVersjoner lovVersjon) {
        if (LovVersjoner.PROP15L2122.equals(lovVersjon)) {
            return new OpptjeningsperiodevilkårParametre(Period.ofMonths(10), Period.ofWeeks(12), Period.ofWeeks(3), Period.ofWeeks(2));
        } else {
            return new OpptjeningsperiodevilkårParametre(Period.ofMonths(10), Period.ofWeeks(12), Period.ofWeeks(3), Period.ofWeeks(0));
        }
    }

    public static OpptjeningsperiodevilkårParametre vilkårparametreSvangerskapspenger() {
        return new OpptjeningsperiodevilkårParametre(Period.ofDays(28), null, null, null);
    }

}
