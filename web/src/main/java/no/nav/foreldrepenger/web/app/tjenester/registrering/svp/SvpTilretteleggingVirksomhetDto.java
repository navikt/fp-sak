package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("VI")
public class SvpTilretteleggingVirksomhetDto extends SvpTilretteleggingArbeidsforholdDto {

    private String organisasjonsnummer;

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }
}
