package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

public abstract class FaktaUttakDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 1, max = 400)
    private List<@Valid FaktaUttakPeriodeDto> perioder;

    FaktaUttakDto() {
        // jackson
    }

    public List<FaktaUttakPeriodeDto> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<FaktaUttakPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_INGEN_PERIODER_KODE)
    public static class IngenPerioderDto extends FaktaUttakDto {


        public IngenPerioderDto() {
            super();
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET_KODE)
    public static class GraderingUkjentAktivitetDto extends FaktaUttakDto {

        public GraderingUkjentAktivitetDto() {
            super();
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG_KODE)
    public static class GraderingAktivitetUtenBGDto extends FaktaUttakDto {

        public GraderingAktivitetUtenBGDto() {
            super();
        }
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO_KODE)
    public static class ManueltSattStartdatoDto extends FaktaUttakDto {

        public ManueltSattStartdatoDto() {
            super();
        }
    }
}
