package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType.DØD;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class DødForretningshendelse extends Forretningshendelse {

    private List<AktørId> aktørIdListe;
    private LocalDate dødsdato;

    public DødForretningshendelse(List<AktørId> aktørIdListe, LocalDate dødsdato) {
        super(DØD);
        this.aktørIdListe = aktørIdListe;
        this.dødsdato = dødsdato;
    }

    public List<AktørId> getAktørIdListe() {
        return aktørIdListe;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }
}
