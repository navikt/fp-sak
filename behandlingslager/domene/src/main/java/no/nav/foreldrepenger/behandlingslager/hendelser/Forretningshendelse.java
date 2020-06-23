package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.List;

import no.nav.foreldrepenger.domene.typer.AktørId;

public abstract class Forretningshendelse {
    private List<AktørId> aktørIdListe;

    private Endringstype endringstype;

    protected Forretningshendelse(List<AktørId> aktørIdListe, Endringstype endringstype) {
        this.aktørIdListe = aktørIdListe;
        this.endringstype = endringstype;
    }

    public List<AktørId> getAktørIdListe() {
        return aktørIdListe;
    }

    public Endringstype getEndringstype() {
        return endringstype;
    }
}
