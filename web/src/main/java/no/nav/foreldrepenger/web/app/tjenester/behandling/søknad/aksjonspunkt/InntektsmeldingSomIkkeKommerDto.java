package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import javax.validation.constraints.Pattern;

/**
 * Inntektsmeldinger som søker har rapport at ikke vil komme fra angitt arbeidsgiver
 */
public class InntektsmeldingSomIkkeKommerDto {

    @Pattern(regexp = "[\\d]{9}|[\\d]{11}")
    private String organisasjonsnummer;

    // AktørId (13-tall) for person-arbeidsgiver
    @Pattern(regexp = "\\d{13}")
    private String aktørId;

    private boolean brukerHarSagtAtIkkeKommer;

    public InntektsmeldingSomIkkeKommerDto() { // NOSONAR
        // Jackson
    }

    public InntektsmeldingSomIkkeKommerDto(String organisasjonsnummer, boolean brukerHarSagtAtIkkeKommer) {
        this.organisasjonsnummer = organisasjonsnummer;
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public boolean isBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    public String getAktørId() {
        return aktørId;
    }
}
