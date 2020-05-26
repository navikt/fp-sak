package no.nav.foreldrepenger.familiehendelse.dødsfall;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class DødfødselForretningshendelse extends Forretningshendelse {

    private LocalDate dødfødselsdato;

    public DødfødselForretningshendelse(List<AktørId> aktørIdListe, LocalDate dødfødselsdato) {
        super(aktørIdListe);
        this.dødfødselsdato = dødfødselsdato;
    }

    public LocalDate getDødfødselsdato() {
        return dødfødselsdato;
    }
}
