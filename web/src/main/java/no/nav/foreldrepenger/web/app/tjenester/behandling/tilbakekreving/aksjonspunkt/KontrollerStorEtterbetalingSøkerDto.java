package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_STOR_ETTERBETALING_SØKER_KODE)
public class KontrollerStorEtterbetalingSøkerDto extends BekreftetAksjonspunktDto {

    public KontrollerStorEtterbetalingSøkerDto() {
        // For Jackson
    }

    public KontrollerStorEtterbetalingSøkerDto(String begrunnelse) {
        super(begrunnelse);
    }
}
