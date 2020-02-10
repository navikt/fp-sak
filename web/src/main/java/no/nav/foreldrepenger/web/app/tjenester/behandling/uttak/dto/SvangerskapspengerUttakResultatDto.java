package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

public class SvangerskapspengerUttakResultatDto {
    private List<SvangerskapspengerUttakResultatArbeidsforholdDto> uttaksResultatArbeidsforhold;

    public SvangerskapspengerUttakResultatDto(List<SvangerskapspengerUttakResultatArbeidsforholdDto> uttaksResultatArbeidsforhold) {
        this.uttaksResultatArbeidsforhold = uttaksResultatArbeidsforhold;
    }

    public List<SvangerskapspengerUttakResultatArbeidsforholdDto> getUttaksResultatArbeidsforhold() {
        return uttaksResultatArbeidsforhold;
    }
}
