package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SvpTilretteleggingDto {

    private LocalDate termindato;

    private LocalDate fødselsdato;

    private List<SvpArbeidsforholdDto> arbeidsforholdListe = new ArrayList<>();

    public LocalDate getTermindato() {
        return termindato;
    }

    public void setTermindato(LocalDate termindato) {
        this.termindato = termindato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public List<SvpArbeidsforholdDto> getArbeidsforholdListe() {
        return arbeidsforholdListe;
    }

    public void setArbeidsforholdListe(List<SvpArbeidsforholdDto> arbeidsforholdListe) {
        this.arbeidsforholdListe = arbeidsforholdListe;
    }
}
