package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.List;

import no.nav.foreldrepenger.domene.typer.AktørId;

public abstract class Forretningshendelse {
    private List<AktørId> aktørIdListe;

    protected Forretningshendelse(List<AktørId> aktørIdListe) {
        this.aktørIdListe = aktørIdListe;
    }

    public List<AktørId> getAktørIdListe() {
        return aktørIdListe;
    }
}
