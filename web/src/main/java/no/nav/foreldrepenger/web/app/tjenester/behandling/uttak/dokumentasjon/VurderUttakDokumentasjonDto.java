package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_UTTAK_DOKUMENTASJON_KODE)
class VurderUttakDokumentasjonDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1, max = 200)
    private List<DokumentasjonVurderingBehovDto> vurderingBehov;

    List<DokumentasjonVurderingBehovDto> getVurderingBehov() {
        return vurderingBehov == null ? List.of() : vurderingBehov;
    }

    public void setVurderingBehov(List<DokumentasjonVurderingBehovDto> vurderingBehov) {
        this.vurderingBehov = vurderingBehov;
    }
}
