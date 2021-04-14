package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class ArbeidsgiverOpplysningerDto implements AbacDto {

    private final String referanse;
    private final String identifikator;
    private final String navn;
    private LocalDate fødselsdato;
    private boolean erPrivatPerson;

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

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett(); // tom, i praksis rollebasert tilgang på JSON-feed
    }

}
