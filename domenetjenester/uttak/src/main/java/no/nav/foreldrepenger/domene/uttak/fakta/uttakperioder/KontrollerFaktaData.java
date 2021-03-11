package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import java.util.List;

public class KontrollerFaktaData {

    private final List<KontrollerFaktaPeriode> perioder;

    public KontrollerFaktaData(List<KontrollerFaktaPeriode> perioder) {
        this.perioder = perioder;
    }

    public List<KontrollerFaktaPeriode> getPerioder() {
        return perioder;
    }
}
