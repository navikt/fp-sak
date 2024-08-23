package no.nav.foreldrepenger.domene.aksjonspunkt;

public class BeregningAktivitetEndring {

    private final BeregningAktivitetNøkkel aktivitetNøkkel;
    private final ToggleEndring skalBrukesEndring;
    private final DatoEndring tomDatoEndring;

    public BeregningAktivitetEndring(BeregningAktivitetNøkkel aktivitetNøkkel,
                                     ToggleEndring skalBrukesEndring,
                                     DatoEndring tomDatoEndring) {
        this.aktivitetNøkkel = aktivitetNøkkel;
        this.skalBrukesEndring = skalBrukesEndring;
        this.tomDatoEndring = tomDatoEndring;
    }

    public BeregningAktivitetNøkkel getAktivitetNøkkel() {
        return aktivitetNøkkel;
    }

    public ToggleEndring getSkalBrukesEndring() {
        return skalBrukesEndring;
    }

    public DatoEndring getTomDatoEndring() {
        return tomDatoEndring;
    }
}
