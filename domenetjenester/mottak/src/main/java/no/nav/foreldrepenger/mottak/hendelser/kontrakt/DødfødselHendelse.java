package no.nav.foreldrepenger.mottak.hendelser.kontrakt;

import java.time.LocalDate;
import java.util.List;

public class DødfødselHendelse extends Hendelse {

    private List<String> aktørIdListe;
    private LocalDate dødfødselsdato;

    public DødfødselHendelse() {
        super("DØDFØDSEL");
        // Jackson
    }

    public DødfødselHendelse(List<String> aktørIdListe, LocalDate dødfødselsdato) {
        super("DØDFØDSEL");
        this.aktørIdListe = aktørIdListe;
        this.dødfødselsdato = dødfødselsdato;
    }

    public List<String> getAktørIdListe() {
        return aktørIdListe;
    }

    public LocalDate getDødfødselsdato() {
        return dødfødselsdato;
    }
    
}
