package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_LOVLIG_OPPHOLD_KODE)
public class BekreftLovligOppholdVurderingDto extends BekreftedePerioderMalDto {


    @SuppressWarnings("unused") // NOSONAR
    private BekreftLovligOppholdVurderingDto() {
        // For Jackson
    }

    public BekreftLovligOppholdVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) { // NOSONAR
        super(begrunnelse, bekreftedePerioder);
    }



}
