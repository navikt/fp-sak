package no.nav.foreldrepenger.datavarehus;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class StønadsstatistikkTjeneste {

    private BehandlingRepository behandlingRepository;


    @Inject
    public StønadsstatistikkTjeneste(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    StønadsstatistikkTjeneste() {
        //CDI
    }

    public StønadsstatistikkVedtak genererVedtak(UUID behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var fagsak = behandling.getFagsak();
        return new StønadsstatistikkVedtak(fagsak.getYtelseType(), fagsak.getSaksnummer().getVerdi());
    }

    //TODO
    public record StønadsstatistikkVedtak(FagsakYtelseType fagsakYtelseType, String saksnummer) {

    }
}
