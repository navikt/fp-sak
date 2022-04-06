package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

public abstract class FaktaUttakDto extends BekreftetAksjonspunktDto {

    @Valid
    @Size(min = 1, max = 1000)
    private List<BekreftetOppgittPeriodeDto> bekreftedePerioder = new ArrayList<>();

    @Valid
    @Size(max = 1000)
    private List<SlettetUttakPeriodeDto> slettedePerioder = new ArrayList<>();

    FaktaUttakDto() { //NOSONAR
        // jackson
    }

    public FaktaUttakDto(String begrunnelse) {
        super(begrunnelse);
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

    @JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER_KODE)
    public static class FaktaUttakPerioderDto extends FaktaUttakDto {


        public FaktaUttakPerioderDto() {
            super();
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO_KODE)
    public static class FaktaUttakFørsteUttakDatoDto extends FaktaUttakDto {

        public FaktaUttakFørsteUttakDatoDto() {
            super();
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET_KODE)
    public static class FaktaUttakGraderingUkjentAktivitet extends FaktaUttakDto {

        public FaktaUttakGraderingUkjentAktivitet() {
            super();
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG_KODE)
    public static class FaktaUttakGradeingAktivitetUtenBG extends FaktaUttakDto {

        public FaktaUttakGradeingAktivitetUtenBG() {
            super();
        }
    }
}
