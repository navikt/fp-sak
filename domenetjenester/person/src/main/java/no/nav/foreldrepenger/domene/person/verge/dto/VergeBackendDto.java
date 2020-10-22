package no.nav.foreldrepenger.domene.person.verge.dto;

public class VergeBackendDto {

    private String aktoerId;
    private String navn;
    private String organisasjonsnummer;

    public VergeBackendDto(String aktoerId, String navn, String organisasjonsnummer) {
        this.aktoerId = aktoerId;
        this.navn = navn;
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public VergeBackendDto() { //NOSONAR
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getNavn() {
        return navn;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }
}
