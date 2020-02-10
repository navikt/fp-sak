package no.nav.foreldrepenger.domene.opptjening.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE)
public class MerkOpptjeningUtlandDto extends BekreftetAksjonspunktDto {

    public MerkOpptjeningUtlandDto() {
        //For Jackson
    }

}
