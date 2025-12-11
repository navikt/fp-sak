package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_UTTAK_DOKUMENTASJON_KODE)
class VurderUttakDokumentasjonDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 1, max = 400)
    private List<@Valid @NotNull DokumentasjonVurderingBehovDto> vurderingBehov;

    public VurderUttakDokumentasjonDto() {
    }

    public VurderUttakDokumentasjonDto(String begrunnelse, List<DokumentasjonVurderingBehovDto> vurderingBehov) {
        super(begrunnelse);
        this.vurderingBehov = vurderingBehov;
    }

    List<DokumentasjonVurderingBehovDto> getVurderingBehov() {
        return vurderingBehov == null ? List.of() : vurderingBehov;
    }

    public void setVurderingBehov(List<DokumentasjonVurderingBehovDto> vurderingBehov) {
        this.vurderingBehov = vurderingBehov;
    }
}
