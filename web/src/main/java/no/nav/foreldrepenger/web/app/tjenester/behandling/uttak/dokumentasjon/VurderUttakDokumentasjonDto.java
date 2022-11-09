package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;

class VurderUttakDokumentasjonDto extends BekreftetAksjonspunktDto {

    private final List<DokumentasjonVurderingBehovDto> vurderingBehov;

    VurderUttakDokumentasjonDto(@Valid @NotNull @Size(min = 1, max = 200) List<DokumentasjonVurderingBehovDto> vurderingBehov) {
        this.vurderingBehov = vurderingBehov;
    }

    List<DokumentasjonVurderingBehovDto> getVurderingBehov() {
        return vurderingBehov;
    }
}
