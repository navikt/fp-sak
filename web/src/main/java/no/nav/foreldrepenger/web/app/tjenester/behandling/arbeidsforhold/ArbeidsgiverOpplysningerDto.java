package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import java.time.LocalDate;

public class ArbeidsgiverOpplysningerDto {

    private final String referanse;
    private final String identifikator;
    private final String navn;
    private LocalDate fødselsdato;

    public ArbeidsgiverOpplysningerDto(String referanse, String identifikator, String navn, LocalDate fødselsdato) {
        this.identifikator = identifikator;
        this.referanse = referanse;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
    }

    public ArbeidsgiverOpplysningerDto(String identifikator, String navn) {
        this.referanse = identifikator;
        this.identifikator = identifikator;
        this.navn = navn;
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
}
