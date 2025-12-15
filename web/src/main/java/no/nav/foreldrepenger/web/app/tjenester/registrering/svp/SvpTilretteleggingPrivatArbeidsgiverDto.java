package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import jakarta.validation.constraints.Pattern;

import no.nav.vedtak.util.InputValideringRegex;

public class SvpTilretteleggingPrivatArbeidsgiverDto extends SvpTilretteleggingArbeidsforholdDto {

    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String arbeidsgiverIdentifikator;

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }
}
