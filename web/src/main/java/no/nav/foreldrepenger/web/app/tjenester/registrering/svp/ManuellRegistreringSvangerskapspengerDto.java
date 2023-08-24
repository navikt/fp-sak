package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.MedInntektArbeidYtelseRegistrering;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE)
public class ManuellRegistreringSvangerskapspengerDto extends MedInntektArbeidYtelseRegistrering {

    @Valid
    private List<SvpTilretteleggingArbeidsforholdDto> tilretteleggingArbeidsforhold;


    public List<SvpTilretteleggingArbeidsforholdDto> getTilretteleggingArbeidsforhold() {
        return tilretteleggingArbeidsforhold;
    }

    public void setTilretteleggingArbeidsforhold(List<SvpTilretteleggingArbeidsforholdDto> tilretteleggingArbeidsforhold) {
        this.tilretteleggingArbeidsforhold = tilretteleggingArbeidsforhold;
    }

}
