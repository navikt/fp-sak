package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE)
public class KontrollerRevurderingsBehandlingDto extends BekreftetAksjonspunktDto {

    public KontrollerRevurderingsBehandlingDto() {
        //For Jackson
    }

}
