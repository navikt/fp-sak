package no.nav.foreldrepenger.domene.person.verge.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;

public class VergeBackendDto {

    private String aktoerId;
    private String navn;
    private String organisasjonsnummer;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;
    private VergeType vergeType;

    public VergeBackendDto(String aktoerId, String navn, String organisasjonsnummer, LocalDate gyldigFom, LocalDate gyldigTom, VergeType vergeType) {
        this.aktoerId = aktoerId;
        this.navn = navn;
        this.organisasjonsnummer = organisasjonsnummer;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
        this.vergeType = vergeType;
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

    public VergeType getVergeType() {
        return vergeType;
    }
}
