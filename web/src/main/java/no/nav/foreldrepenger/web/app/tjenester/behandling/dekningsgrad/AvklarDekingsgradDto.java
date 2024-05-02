package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_DEKNINGSGRAD_KODE)
public class AvklarDekingsgradDto extends BekreftetAksjonspunktDto {

    @Size(min = 80, max = 100)
    private final int avklartDekningsgrad;

    public AvklarDekingsgradDto(String begrunnelse, int avklartDekningsgrad) {
        super(begrunnelse);
        this.avklartDekningsgrad = avklartDekningsgrad;
    }

    public int avklartDekningsgrad() {
        return avklartDekningsgrad;
    }
}
