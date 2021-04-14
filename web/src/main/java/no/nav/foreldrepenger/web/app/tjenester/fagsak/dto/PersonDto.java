package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;

public class PersonDto {

    private String navn;
    private String fødselsnummer;
    private NavBrukerKjønn kjønn;
    private String diskresjonskode;
    private LocalDate fødselsdato;
    private LocalDate dodsdato;
    private Språkkode språkkode;
    private String aktørId;

    public PersonDto() {
        // Injiseres i test
    }

    public PersonDto(String aktørid, String navn, String fødselsnummer, NavBrukerKjønn navBrukerKjønn, String diskresjonskode,
                     LocalDate fødselsdato, LocalDate dodsdato, Språkkode språkkode) {
        this.navn = navn;
        this.fødselsnummer = fødselsnummer;
        this.kjønn = navBrukerKjønn;
        this.diskresjonskode = diskresjonskode;
        this.fødselsdato = fødselsdato;
        this.dodsdato = dodsdato;
        this.språkkode = språkkode;
        this.aktørId = aktørid;
    }

    public String getNavn() {
        return navn;
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
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
        return "PersonDto{" +
            "aktørId='" + aktørId + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var personDto = (PersonDto) o;
        return Objects.equals(aktørId, personDto.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }
}
