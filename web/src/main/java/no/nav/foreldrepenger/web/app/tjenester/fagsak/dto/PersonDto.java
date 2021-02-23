package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;

public class PersonDto {

    private String navn;
    private Integer alder;
    private String personnummer;
    private NavBrukerKjønn kjønn;
    private Boolean erKvinne;
    private PersonstatusType personstatusType;
    private String diskresjonskode;
    private LocalDate fodselsdato;
    private LocalDate dodsdato;
    private boolean erDod;
    private Språkkode språkkode;

    public PersonDto() {
        // Injiseres i test
    }

    public PersonDto(String navn, String personnummer, NavBrukerKjønn navBrukerKjønn, PersonstatusType personstatusType, String diskresjonskode,
                     LocalDate fodselsdato, LocalDate dodsdato, Språkkode språkkode) {
        this.navn = navn;
        this.alder = (int) ChronoUnit.YEARS.between(fodselsdato, LocalDate.now());
        this.personnummer = personnummer;
        this.erKvinne = NavBrukerKjønn.KVINNE.equals(navBrukerKjønn);
        this.kjønn = navBrukerKjønn;
        this.personstatusType = personstatusType;
        this.diskresjonskode = diskresjonskode;
        this.fodselsdato = fodselsdato;
        this.dodsdato = dodsdato;
        this.erDod = dodsdato != null || PersonstatusType.erDød(personstatusType);
        this.språkkode = språkkode;
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

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public Boolean getErKvinne() {
        return erKvinne;
    }

    public PersonstatusType getPersonstatusType() {
        return personstatusType;
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

    public boolean getErDod() {
        return erDod;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
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
