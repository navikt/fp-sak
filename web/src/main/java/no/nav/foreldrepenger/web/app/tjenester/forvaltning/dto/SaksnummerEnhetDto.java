package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

public class SaksnummerEnhetDto {

    private String saksnummer;
    private String behandlendeEnhet;

    public SaksnummerEnhetDto(String saksnummer, String behandlendeEnhet) {
        this.saksnummer = saksnummer;
        this.behandlendeEnhet = behandlendeEnhet;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }

    @Override
    public String toString() {
        return "SaksnummerEnhetDto{" +
            "saksnummer='" + saksnummer + '\'' +
            ", behandlendeEnhet='" + behandlendeEnhet + '\'' +
            '}';
    }
}
