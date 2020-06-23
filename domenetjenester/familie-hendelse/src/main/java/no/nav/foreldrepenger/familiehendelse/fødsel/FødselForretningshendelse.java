package no.nav.foreldrepenger.familiehendelse.fødsel;


import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class FødselForretningshendelse extends Forretningshendelse {

    private LocalDate fødselsdato;

    public FødselForretningshendelse(List<AktørId> aktørIdListe, LocalDate fødselsdato, Endringstype endringstype) {
        super(aktørIdListe, endringstype);
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }
}
