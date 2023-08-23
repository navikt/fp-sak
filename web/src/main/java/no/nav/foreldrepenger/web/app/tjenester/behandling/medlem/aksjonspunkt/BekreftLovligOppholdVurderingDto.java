package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_LOVLIG_OPPHOLD_KODE)
public class BekreftLovligOppholdVurderingDto extends BekreftedePerioderMalDto {


    @SuppressWarnings("unused")
    private BekreftLovligOppholdVurderingDto() {
        // For Jackson
    }

    public BekreftLovligOppholdVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) {
        super(begrunnelse, bekreftedePerioder);
    }



}
