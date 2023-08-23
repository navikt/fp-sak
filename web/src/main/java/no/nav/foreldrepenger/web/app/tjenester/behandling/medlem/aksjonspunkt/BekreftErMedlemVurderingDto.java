package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE)
public class BekreftErMedlemVurderingDto extends BekreftedePerioderMalDto {

    BekreftErMedlemVurderingDto() {
        // For Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) {
        super(begrunnelse, bekreftedePerioder);

    }

}
