package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OPPHOLDSRETT_KODE)
public class BekreftOppholdsrettVurderingDto extends BekreftedePerioderMalDto {


    @SuppressWarnings("unused")
    private BekreftOppholdsrettVurderingDto() {
        // For Jackson
    }

    public BekreftOppholdsrettVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) {
        super(begrunnelse, bekreftedePerioder);
    }

}
