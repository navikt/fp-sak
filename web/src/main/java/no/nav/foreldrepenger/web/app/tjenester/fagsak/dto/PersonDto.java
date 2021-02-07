package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonDto {

    @JsonProperty("navn")
    private String navn;

    @JsonProperty("alder")
    private Integer alder;

    @JsonProperty("personnummer")
    private String personnummer;

    @JsonProperty("erKvinne")
    private Boolean erKvinne;

    @JsonProperty("personstatusType")
    private PersonstatusType personstatusType;

    @JsonProperty("diskresjonskode")
    private String diskresjonskode;

    @JsonProperty("fodselsdato")
    private LocalDate fodselsdato;

    @JsonProperty("dodsdato")
    private LocalDate dodsdato;

    public PersonDto() {
        // Injiseres i test
    }

    public PersonDto(String navn, String personnummer, boolean erKvinne, PersonstatusType personstatusType, String diskresjonskode,
                     LocalDate fodselsdato, LocalDate dodsdato) {
        this.navn = navn;
        this.alder = (int) ChronoUnit.YEARS.between(fodselsdato, LocalDate.now());
        this.personnummer = personnummer;
        this.erKvinne = erKvinne;
        this.personstatusType = personstatusType;
        this.diskresjonskode = diskresjonskode;
        this.fodselsdato = fodselsdato;
        this.dodsdato = dodsdato;
    }

    public String getNavn() {
        return navn;
    }

    public Integer getAlder() {
        return alder;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public Boolean getErKvinne() {
        return erKvinne;
    }

    public PersonstatusType getPersonstatusType() {
        return personstatusType;
    }

    @JsonGetter
    public Boolean getErDod() {
        return PersonstatusType.erDød(personstatusType);
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public LocalDate getDodsdato() {
        return dodsdato;
    }

    @Override
    public String toString() {
        return "<navn='" + navn + '\'' +
            ", alder=" + alder +
            ", erKvinne=" + erKvinne +
            '>';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonDto personDto = (PersonDto) o;
        return Objects.equals(navn, personDto.navn) &&
            Objects.equals(personnummer, personDto.personnummer) &&
            Objects.equals(erKvinne, personDto.erKvinne) &&
            Objects.equals(fodselsdato, personDto.fodselsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(navn, personnummer, erKvinne, fodselsdato);
    }
}
