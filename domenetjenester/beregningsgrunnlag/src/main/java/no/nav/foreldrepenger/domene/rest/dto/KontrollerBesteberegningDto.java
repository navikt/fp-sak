package no.nav.foreldrepenger.domene.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_AUTOMATISK_BESTEBEREGNING_KODE)
public class KontrollerBesteberegningDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    private Boolean besteberegningErKorrekt;

    KontrollerBesteberegningDto() {
        // For Jackson
    }

    public KontrollerBesteberegningDto(String begrunnelse, @Valid @NotNull Boolean besteberegningErKorrekt) {
        super(begrunnelse);
        this.besteberegningErKorrekt = besteberegningErKorrekt;
    }

    public Boolean getBesteberegningErKorrekt() {
        return besteberegningErKorrekt;
    }
}
