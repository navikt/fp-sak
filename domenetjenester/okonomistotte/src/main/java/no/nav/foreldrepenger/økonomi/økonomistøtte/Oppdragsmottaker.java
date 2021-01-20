package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragsmottakerStatus;

public class Oppdragsmottaker {

    private static final String MOTTAKER_STATUS = "mottakerStatus";

    private boolean bruker;
    private String fnr;
    private String orgnr;
    private OppdragsmottakerStatus status;

    public Oppdragsmottaker(String id, boolean bruker) {
        Objects.requireNonNull(id, "id");
        this.bruker = bruker;
        if (bruker) {
            this.fnr = id;
        } else {
            this.orgnr = id;
        }
    }

    public boolean erBruker() {
        return bruker;
    }

    public String getFnr() {
        if (!bruker) {
            throw new IllegalStateException("Mottaker er ikke bruker");
        }
        return fnr;
    }

    public String getOrgnr() {
        if (bruker) {
            throw new IllegalStateException("Mottaker er bruker");
        }
        return orgnr;
    }

    public String getId() {
        return (bruker ? fnr : orgnr);
    }

    public OppdragsmottakerStatus getStatus() {
        return status;
    }

    public void setStatus(OppdragsmottakerStatus status) {
        this.status = status;
    }

    public boolean erStatusNy() {
        Objects.requireNonNull(status, MOTTAKER_STATUS);
        return OppdragsmottakerStatus.NY.equals(this.status);
    }

    public boolean erStatusEndret() {
        Objects.requireNonNull(status, MOTTAKER_STATUS);
        return OppdragsmottakerStatus.ENDR.equals(this.status);
    }

    public boolean erStatusUendret() {
        Objects.requireNonNull(status, MOTTAKER_STATUS);
        return OppdragsmottakerStatus.UENDR.equals(this.status);
    }

    public boolean erStatusOpphør() {
        Objects.requireNonNull(status, MOTTAKER_STATUS);
        return OppdragsmottakerStatus.OPPH.equals(this.status);
    }

    public String getIdMaskert() {
        return "xxxxxx" + getId().substring(6);
    }

    @Override
    public String toString() {
        return "Oppdragsmottaker{" +
            "orgnr='" + orgnr + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof Oppdragsmottaker)) return false;
        Oppdragsmottaker other = (Oppdragsmottaker) arg0;
        return bruker == other.bruker
            && Objects.equals(fnr, other.fnr)
            && Objects.equals(orgnr, other.orgnr);
    }

    @Override
    public int hashCode() {
        return bruker ? fnr.hashCode() : orgnr.hashCode();
    }
}
