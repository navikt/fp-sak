package no.nav.foreldrepenger.domene.person.verge.dto;

import java.time.LocalDate;

public class VergeBackendDto {

    private String aktoerId;
    private String navn;
    private String organisasjonsnummer;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    public VergeBackendDto(String aktoerId, String navn, String organisasjonsnummer, LocalDate gyldigFom, LocalDate gyldigTom) {
        this.aktoerId = aktoerId;
        this.navn = navn;
        this.organisasjonsnummer = organisasjonsnummer;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public String getNavn() {
        return navn;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public LocalDate getGyldigFom() {
        return gyldigFom;
    }

    public LocalDate getGyldigTom() {
        return gyldigTom;
    }
}
