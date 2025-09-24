package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.adopsjon.dto;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsOvertakelse;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON_KODE)
public class BekreftDokumentertDatoAksjonspunktDto extends BekreftetAksjonspunktDto implements OmsorgsOvertakelse {


    @NotNull
    private LocalDate omsorgsovertakelseDato;

    @Valid
    @Size(max = 9)
    private Map<Integer, LocalDate> fodselsdatoer;

    BekreftDokumentertDatoAksjonspunktDto() {
        // For Jackson
    }

    public BekreftDokumentertDatoAksjonspunktDto(String begrunnelse, LocalDate omsorgsovertakelseDato,
                                                 Map<Integer, LocalDate> fodselsdatoer) {

        super(begrunnelse);
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
        this.fodselsdatoer = fodselsdatoer;
    }


    @Override
    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public Map<Integer, LocalDate> getFodselsdatoer() {
        return fodselsdatoer;
    }

}
