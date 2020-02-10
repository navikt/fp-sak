package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OPPHOLDSRETT_KODE)
public class BekreftOppholdsrettVurderingDto extends BekreftedePerioderMalDto {


    @SuppressWarnings("unused") // NOSONAR
    private BekreftOppholdsrettVurderingDto() {
        // For Jackson
    }

    public BekreftOppholdsrettVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) { // NOSONAR
        super(begrunnelse, bekreftedePerioder);
    }

}
