package no.nav.foreldrepenger.mottak.hendelser.kontrakt;

import java.time.LocalDate;
import java.util.List;

public class DødHendelse extends Hendelse {

    private List<String> aktørIdListe;
    private LocalDate dødsdato;

    public DødHendelse() {
        super("DØD");
        // Jackson
    }

    public DødHendelse(List<String> aktørIdListe, LocalDate dødsdato) {
        super("DØD");
        this.aktørIdListe = aktørIdListe;
        this.dødsdato = dødsdato;
    }

    public List<String> getAktørIdListe() {
        return aktørIdListe;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }
}
