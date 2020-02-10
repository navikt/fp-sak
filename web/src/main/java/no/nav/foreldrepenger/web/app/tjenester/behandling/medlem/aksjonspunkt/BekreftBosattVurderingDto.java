package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OM_ER_BOSATT_KODE)
public class BekreftBosattVurderingDto extends BekreftedePerioderMalDto {


    BekreftBosattVurderingDto() { // NOSONAR
        // For Jackson
    }

    public BekreftBosattVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) { // NOSONAR
        super(begrunnelse, bekreftedePerioder);
    }



}
