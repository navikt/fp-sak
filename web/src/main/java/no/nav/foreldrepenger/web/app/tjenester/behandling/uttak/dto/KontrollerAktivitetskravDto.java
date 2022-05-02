package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_AKTIVITETSKRAV)
public class KontrollerAktivitetskravDto extends BekreftetAksjonspunktDto {

    @Size(max = 200)
    @Valid
    private List<KontrollerAktivitetskravPeriodeDto> avklartePerioder;

    public KontrollerAktivitetskravDto() {
        //Jackson
    }

    public List<KontrollerAktivitetskravPeriodeDto> getAvklartePerioder() {
        return avklartePerioder == null ? List.of() : avklartePerioder;
    }

    public void setAvklartePerioder(List<KontrollerAktivitetskravPeriodeDto> avklartePerioder) {
        this.avklartePerioder = avklartePerioder;
    }
}
