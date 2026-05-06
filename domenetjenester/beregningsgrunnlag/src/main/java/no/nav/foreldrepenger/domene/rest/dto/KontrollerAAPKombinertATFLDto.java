package no.nav.foreldrepenger.domene.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AAP_KOMBINERT_ATFL_KODE)
public class KontrollerAAPKombinertATFLDto extends BekreftetAksjonspunktDto {

    KontrollerAAPKombinertATFLDto() {
        // For Jackson
    }

    public KontrollerAAPKombinertATFLDto(String begrunnelse) {
        super(begrunnelse);
    }
}
