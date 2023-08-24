package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public abstract class BekreftedePerioderMalDto extends BekreftetAksjonspunktDto {
    private List<BekreftedePerioderDto> bekreftedePerioder;

    public BekreftedePerioderMalDto() {
        // For Jackson
    }

    public BekreftedePerioderMalDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) {
        super(begrunnelse);
        this.bekreftedePerioder = bekreftedePerioder;
    }

    public List<BekreftedePerioderDto> getBekreftedePerioder() {
        return bekreftedePerioder;
    }
}
