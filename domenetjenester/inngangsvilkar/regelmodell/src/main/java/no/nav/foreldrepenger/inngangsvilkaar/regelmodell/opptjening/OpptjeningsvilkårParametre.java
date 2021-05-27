package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.Period;
/*
 * Minste antall måneder/dager som kreves godkjent. Hvis eksakt match måneder, så sjekkes også dager.
 * Minste godkjente inntekt i en periode.
 * Maks periode i en mellomliggende periode for et arbeidsforhold for at den skal kunne regnes med.
 * Minste periode for en foregående periode i et arbeidsforhold for at en mellomliggende periode skal regnes med.
 * Om man skal godkjenne aktiviteter med anntatt framtidig inntekt
 * Periode før behandlingstidspunkt hvor arbeid kan antas godkjent selv om ikke inntekter er rapportert inn ennå.
 */
public record OpptjeningsvilkårParametre(int minsteAntallMånederGodkjent,
                                         int minsteAntallDagerGodkjent,
                                         Long minsteInntekt,
                                         Period maksMellomliggendePeriodeForArbeidsforhold,
                                         Period minForegåendeForMellomliggendePeriodeForArbeidsforhold,
                                         boolean skalGodkjenneBasertPåAntatt,
                                         Period periodeAntattGodkjentFørBehandlingstidspunkt) {

    public static OpptjeningsvilkårParametre opptjeningsparametreForeldrepenger() {
        return new OpptjeningsvilkårParametre(5, 26, 1L,
            Period.ofDays(14), Period.ofWeeks(4), false, Period.ofMonths(2));
    }

    public static OpptjeningsvilkårParametre opptjeningsparametreSvangerskapspenger() {
        return new OpptjeningsvilkårParametre(0, 28, 1L,
            Period.ofDays(14), Period.ofWeeks(4), true, Period.ofMonths(2));
    }

}
