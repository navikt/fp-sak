package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.time.LocalDate;

public class ArbeidsgiverOpplysninger {

    private final String identifikator;
    private final String navn;
    private LocalDate fødselsdato; // Fødselsdato for privatperson som arbeidsgiver

    public ArbeidsgiverOpplysninger(String identifikator, String navn, LocalDate fødselsdato) {
        this.identifikator = identifikator;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
    }

    public ArbeidsgiverOpplysninger(String identifikator, String navn) {
        this.identifikator = identifikator;
        this.navn = navn;
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
