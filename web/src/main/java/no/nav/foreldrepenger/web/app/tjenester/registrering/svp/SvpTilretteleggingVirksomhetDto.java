package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

public class SvpTilretteleggingVirksomhetDto extends SvpTilretteleggingArbeidsforholdDto {

    private String organisasjonsnummer;

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }
}
