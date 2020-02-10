package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataBarnDto;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataForeldreDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_VILKÅR_FOR_FORELDREANSVAR_KODE)
public class AvklarFaktaForForeldreansvarAksjonspunktDto extends BekreftetAksjonspunktDto implements OmsorgsOvertakelse {


    @NotNull
    private LocalDate omsorgsovertakelseDato;


    @NotNull
    private LocalDate foreldreansvarDato;

    @Min(1)
    @Max(9)
    private Integer antallBarn;

    @Valid
    @NotNull
    @Size(max = 9)
    private List<AvklartDataBarnDto> barn = new ArrayList<>();

    @Valid
    @NotNull
    @Size(max = 9)
    private List<AvklartDataForeldreDto> foreldre = new ArrayList<>();


    @Override
    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public LocalDate getForeldreansvarDato() {
        return foreldreansvarDato;
    }

    public void setForeldreansvarDato(LocalDate foreldreansvarDato) {
        this.foreldreansvarDato = foreldreansvarDato;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    public List<AvklartDataBarnDto> getBarn() {
        return barn;
    }

    public void setBarn(List<AvklartDataBarnDto> barn) {
        this.barn = barn;
    }

    public List<AvklartDataForeldreDto> getForeldre() {
        return foreldre;
    }

    public void setForeldre(List<AvklartDataForeldreDto> foreldre) {
        this.foreldre = foreldre;
    }
}
