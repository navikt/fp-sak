package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE)
public class BekreftErMedlemVurderingDto extends BekreftedePerioderMalDto {

    BekreftErMedlemVurderingDto() { // NOSONAR
        // For Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) { // NOSONAR
        super(begrunnelse, bekreftedePerioder);

    }

}
