package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType.DØDFØDSEL;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class DødfødselForretningshendelse extends Forretningshendelse {

    private List<AktørId> aktørIdListe;
    private LocalDate dødfødselsdato;

    public DødfødselForretningshendelse(List<AktørId> aktørIdListe, LocalDate dødfødselsdato) {
        super(DØDFØDSEL);
        this.aktørIdListe = aktørIdListe;
        this.dødfødselsdato = dødfødselsdato;
    }

    public List<AktørId> getAktørIdListe() {
        return aktørIdListe;
    }

    public LocalDate getDødfødselsdato() {
        return dødfødselsdato;
    }
}
