package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import java.util.HashMap;
import java.util.Map;

public class ArbeidsgiverOversiktDto {

    private final Map<String, ArbeidsgiverOpplysningerDto> arbeidsgivere;

    public ArbeidsgiverOversiktDto() {
        this.arbeidsgivere = new HashMap<>();
    }

    public ArbeidsgiverOversiktDto(Map<String, ArbeidsgiverOpplysningerDto> arbeidsgivere) {
        this.arbeidsgivere = arbeidsgivere;
    }

    public Map<String, ArbeidsgiverOpplysningerDto> getArbeidsgivere() {
        return arbeidsgivere;
    }
}
