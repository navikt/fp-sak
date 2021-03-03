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
    private String fødselsnummer;
    private String personnummer;
    private NavBrukerKjønn kjønn;
    private Boolean erKvinne;
    private PersonstatusType personstatusType;
    private String diskresjonskode;
    private LocalDate fødselsdato;
    private LocalDate dodsdato;
    private boolean erDod;
    private Språkkode språkkode;
    private String aktørId;

    public PersonDto() {
        // Injiseres i test
    }

    public PersonDto(String aktørid, String navn, String fødselsnummer, NavBrukerKjønn navBrukerKjønn, PersonstatusType personstatusType, String diskresjonskode,
                     LocalDate fødselsdato, LocalDate dodsdato, Språkkode språkkode) {
        this.navn = navn;
        this.alder = fødselsdato != null ? (int) ChronoUnit.YEARS.between(fødselsdato, LocalDate.now()) : 0;
        this.personnummer = fødselsnummer;
        this.fødselsnummer = fødselsnummer;
        this.erKvinne = NavBrukerKjønn.KVINNE.equals(navBrukerKjønn);
        this.kjønn = navBrukerKjønn;
        this.personstatusType = personstatusType;
        this.diskresjonskode = diskresjonskode;
        this.fødselsdato = fødselsdato;
        this.dodsdato = dodsdato;
        this.erDod = dodsdato != null || PersonstatusType.erDød(personstatusType);
        this.språkkode = språkkode;
        this.aktørId = aktørid;
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

    public LocalDate getFødselsdato() {
        return fødselsdato;
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

    public String getAktørId() {
        return aktørId;
    }

    public String getFødselsnummer() {
        return fødselsnummer;
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
        return Objects.equals(aktørId, personDto.aktørId) &&
            Objects.equals(fødselsnummer, personDto.fødselsnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, personnummer);
    }
}
