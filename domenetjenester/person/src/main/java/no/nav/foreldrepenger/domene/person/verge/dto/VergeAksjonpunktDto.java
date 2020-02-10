package no.nav.foreldrepenger.domene.person.verge.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class VergeAksjonpunktDto {

    private PersonIdent fnr;
    private LocalDate fom;
    private LocalDate tom;
    private String vergeTypeKode;
    private String navn;
    private String organisasjonsnummer;

    public VergeAksjonpunktDto(PersonIdent fnr, LocalDate fom, LocalDate tom, String vergeTypeKode,
                               String navn, String organisasjonsnummer) {
        this.fnr = fnr;
        this.fom = fom;
        this.tom = tom;
        this.vergeTypeKode = vergeTypeKode;
        this.navn = navn;
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public PersonIdent getFnr() {
        return fnr;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getVergeTypeKode() {
        return vergeTypeKode;
    }

    public String getNavn() {
        return navn;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }
}
