package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.util.List;

public class BeregningAktiviteterEndring {

    private List<BeregningAktivitetEndring> aktivitetEndringer;

    public BeregningAktiviteterEndring() {
    }

    public BeregningAktiviteterEndring(List<BeregningAktivitetEndring> aktivitetEndringer) {
        this.aktivitetEndringer = aktivitetEndringer;
    }

    public List<BeregningAktivitetEndring> getAktivitetEndringer() {
        return aktivitetEndringer;
    }
}
