package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

import java.util.List;

@ApplicationScoped
public class DokumentasjonVurderingBehovDtoTjeneste {

    private BehandlingRepository behandlingRepository;
    private UttakInputTjeneste uttakInputTjeneste;
    private VurderUttakDokumentasjonAksjonspunktUtleder utleder;

    @Inject
    public DokumentasjonVurderingBehovDtoTjeneste(BehandlingRepository behandlingRepository,
                                                  UttakInputTjeneste uttakInputTjeneste,
                                                  VurderUttakDokumentasjonAksjonspunktUtleder utleder) {
        this.behandlingRepository = behandlingRepository;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.utleder = utleder;
    }

    DokumentasjonVurderingBehovDtoTjeneste() {
        //CDI
    }

    public List<DokumentasjonVurderingBehovDto> lagDtos(UuidDto behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId.getBehandlingUuid());
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        return utleder.utledDokumentasjonVurderingBehov(uttakInput)
            .stream()
            .filter(b -> b.behov() != null)
            .map(DokumentasjonVurderingBehovDto::from)
            .toList();
    }
}
