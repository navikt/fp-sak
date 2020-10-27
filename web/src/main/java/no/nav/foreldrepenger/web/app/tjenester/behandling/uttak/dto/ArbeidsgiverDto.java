package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.domene.typer.AktørId;

public class ArbeidsgiverDto {

    private String identifikator;

    private AktørId aktørId;

    private LocalDate fødselsdato;

    private String navn;

    private String arbeidsgiverReferanse;

    private ArbeidsgiverDto(String identifikator, String navn, AktørId aktørId, LocalDate fødselsdato) {
        this.identifikator = identifikator;
        this.navn = navn;
        this.aktørId = aktørId;
        this.fødselsdato = fødselsdato;
        this.arbeidsgiverReferanse = aktørId != null ? aktørId.getId() : identifikator;
    }

    public static ArbeidsgiverDto virksomhet(String identifikator, String navn) {
        return new ArbeidsgiverDto(identifikator, navn, null, null);
    }

    public static ArbeidsgiverDto person(String navn, AktørId aktørId, LocalDate fødselsdato) {
        return new ArbeidsgiverDto(null, navn, aktørId, fødselsdato);
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public String getNavn() {
        return navn;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    @JsonProperty("virksomhet")
    public boolean isVirksomhet() {
        return aktørId == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsgiverDto that = (ArbeidsgiverDto) o;
        return Objects.equals(identifikator, that.identifikator) &&
            Objects.equals(aktørId, that.aktørId) &&
            Objects.equals(fødselsdato, that.fødselsdato) &&
            Objects.equals(navn, that.navn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifikator, aktørId, fødselsdato, navn);
    }
}
