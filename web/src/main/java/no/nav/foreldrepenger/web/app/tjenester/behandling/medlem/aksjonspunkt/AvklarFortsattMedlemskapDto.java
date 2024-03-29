package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE)
public class AvklarFortsattMedlemskapDto extends BekreftedePerioderMalDto {


    AvklarFortsattMedlemskapDto() {
        // For Jackson
    }

    public AvklarFortsattMedlemskapDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) {
        super(begrunnelse, bekreftedePerioder);
    }

}
