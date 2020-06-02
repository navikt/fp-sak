package no.nav.foreldrepenger.familiehendelse.fødsel;


import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class FødselForretningshendelse extends Forretningshendelse {

    private LocalDate fødselsdato;

    public FødselForretningshendelse(List<AktørId> aktørIdListe, LocalDate fødselsdato) {
        super(aktørIdListe);
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }
}
