package no.nav.foreldrepenger.domene.oppdateringresultat;

public class BeregningAktivitetEndring {

    private BeregningAktivitetNøkkel aktivitetNøkkel;
    private ToggleEndring skalBrukesEndring;
    private DatoEndring tomDatoEndring;

    public BeregningAktivitetEndring() {
    }

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
