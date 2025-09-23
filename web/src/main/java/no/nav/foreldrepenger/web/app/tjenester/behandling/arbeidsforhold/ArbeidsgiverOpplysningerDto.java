package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

public class ArbeidsgiverOpplysningerDto {

    @NotNull private final String referanse;
    @NotNull private final String identifikator;
    @NotNull private final String navn;
    private LocalDate fødselsdato;
    @NotNull private boolean erPrivatPerson;

    public ArbeidsgiverOpplysningerDto(String referanse, String identifikator, String navn, LocalDate fødselsdato) {
        this.identifikator = identifikator;
        this.referanse = referanse;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
        this.erPrivatPerson = true;
    }

    public ArbeidsgiverOpplysningerDto(String identifikator, String navn) {
        this.referanse = identifikator;
        this.identifikator = identifikator;
        this.navn = navn;
        this.erPrivatPerson = false;
    }

    public String getReferanse() {
        return referanse;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public boolean getErPrivatPerson() {
        return erPrivatPerson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ArbeidsgiverOpplysningerDto) o;
        return Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referanse);
    }

}
