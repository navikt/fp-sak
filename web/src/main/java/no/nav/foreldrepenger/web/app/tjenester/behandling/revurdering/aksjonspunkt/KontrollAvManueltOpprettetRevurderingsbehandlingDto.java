package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE)
public class KontrollAvManueltOpprettetRevurderingsbehandlingDto  extends BekreftetAksjonspunktDto {

    public KontrollAvManueltOpprettetRevurderingsbehandlingDto() {
        //For Jackson
    }

}


