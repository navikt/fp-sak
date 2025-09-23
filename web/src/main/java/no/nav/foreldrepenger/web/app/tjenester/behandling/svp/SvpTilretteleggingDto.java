package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SvpTilretteleggingDto {

    private LocalDate termindato;

    private LocalDate fødselsdato;

    @NotNull private List<SvpArbeidsforholdDto> arbeidsforholdListe = new ArrayList<>();

    @NotNull private boolean saksbehandlet;

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

    public void leggTilArbeidsforhold(SvpArbeidsforholdDto arbeidsforhold) {
        this.arbeidsforholdListe.add(arbeidsforhold);
    }

    public boolean isSaksbehandlet() {
        return saksbehandlet;
    }

    public void setSaksbehandlet(boolean saksbehandlet) {
        this.saksbehandlet = saksbehandlet;
    }
}
