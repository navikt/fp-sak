package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import jakarta.validation.constraints.Pattern;

import no.nav.vedtak.util.InputValideringRegex;

public class SvpTilretteleggingVirksomhetDto extends SvpTilretteleggingArbeidsforholdDto {

    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String organisasjonsnummer;

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }
}
