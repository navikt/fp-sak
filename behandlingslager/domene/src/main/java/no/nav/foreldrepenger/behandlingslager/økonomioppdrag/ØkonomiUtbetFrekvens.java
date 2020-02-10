package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

public enum ØkonomiUtbetFrekvens {
    DAG("DAG"),
    UKE("UKE"),
    MÅNED("MND"),
    DAGER14("14DG"),
    ENGANG("ENG");

    private String utbetFrekvens;

    ØkonomiUtbetFrekvens(String utbetFrekvens) {
        this.utbetFrekvens = utbetFrekvens;
    }

    public String getUtbetFrekvens() {
        return utbetFrekvens;
    }
}
