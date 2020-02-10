package no.nav.foreldrepenger.ytelse.beregning.rest;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE)
public class VurderTilbaketrekkDto extends BekreftetAksjonspunktDto {

    private Boolean hindreTilbaketrekk;

    VurderTilbaketrekkDto() {
        // For Jackson
    }

    public VurderTilbaketrekkDto(String begrunnelse, boolean hindreTilbaketrekk) {
        super(begrunnelse);
        this.hindreTilbaketrekk = hindreTilbaketrekk;
    }

    public void setHindreTilbaketrekk(Boolean hindreTilbaketrekk) {
        this.hindreTilbaketrekk = hindreTilbaketrekk;
    }


    public boolean skalHindreTilbaketrekk() {
        return hindreTilbaketrekk;
    }
}
