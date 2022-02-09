package no.nav.foreldrepenger.domene.modell;


import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class Grunnbeløp {

    private long verdi;
    private DatoIntervallEntitet periode;

    public Grunnbeløp(long verdi, DatoIntervallEntitet periode) {
        this.verdi = verdi;
        this.periode = periode;
    }

    public long getVerdi() {
        return verdi;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
