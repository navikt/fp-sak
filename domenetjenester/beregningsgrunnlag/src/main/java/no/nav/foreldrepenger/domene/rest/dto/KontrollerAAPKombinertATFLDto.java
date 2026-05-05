package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AAP_KOMBINERT_ATFL_KODE)
public class KontrollerAAPKombinertATFLDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    private Boolean erBeregningenKorrekt;

    KontrollerAAPKombinertATFLDto() {
        // For Jackson
    }

    public KontrollerAAPKombinertATFLDto(String begrunnelse, @Valid @NotNull Boolean erBeregningenKorrekt) {
        super(begrunnelse);
        this.erBeregningenKorrekt = erBeregningenKorrekt;
    }

    public Boolean getErBeregningenKorrekt() {
        return erBeregningenKorrekt;
    }
}
