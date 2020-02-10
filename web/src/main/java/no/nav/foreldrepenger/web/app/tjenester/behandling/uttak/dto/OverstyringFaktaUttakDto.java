package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

public abstract class OverstyringFaktaUttakDto extends OverstyringAksjonspunktDto {

    @Valid
    @Size(min = 1, max = 1000)
    private List<BekreftetOppgittPeriodeDto> bekreftedePerioder = new ArrayList<>();

    @Valid
    @Size(max = 1000)
    private List<SlettetUttakPeriodeDto> slettedePerioder = new ArrayList<>();

    OverstyringFaktaUttakDto() { //NOSONAR
        // jackson
    }

    public List<BekreftetOppgittPeriodeDto> getBekreftedePerioder() {
        return bekreftedePerioder;
    }

    public void setBekreftedePerioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        this.bekreftedePerioder = bekreftedePerioder;
    }

    public void setSlettedePerioder(List<SlettetUttakPeriodeDto> slettedePerioder) {
        this.slettedePerioder = slettedePerioder;
    }

    public List<SlettetUttakPeriodeDto> getSlettedePerioder() {
        return slettedePerioder;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        //Brukes ikke
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        //Brukes ikke
        return false;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING_KODE)
    public static class SaksbehandlerOverstyrerFaktaUttakDto extends OverstyringFaktaUttakDto {
        //Brukes i overstyringspunkt som saksbehandler opprettet for kontroll av uttaksperioder med revurdering

        public SaksbehandlerOverstyrerFaktaUttakDto() {
           super();
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK_KODE)
    public static class OverstyrerFaktaUttakDto extends OverstyringFaktaUttakDto {
        //Overstyrer overstyrer perioder, unansett av førstegangs eller revurdering

        public OverstyrerFaktaUttakDto() {
            super();
        }
    }
}
