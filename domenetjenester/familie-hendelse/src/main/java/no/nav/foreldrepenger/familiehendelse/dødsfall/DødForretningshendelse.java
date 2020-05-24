package no.nav.foreldrepenger.familiehendelse.dødsfall;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class DødForretningshendelse extends Forretningshendelse {

    private LocalDate dødsdato;

    public DødForretningshendelse(List<AktørId> aktørIdListe, LocalDate dødsdato) {
        super(aktørIdListe);
        this.dødsdato = dødsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }
}
