package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.DØDFØDSEL_HENDELSE)
public class DødfødselForretningshendelseHåndterer implements ForretningshendelseHåndterer {

    private ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    @Inject
    public DødfødselForretningshendelseHåndterer(BehandlingRepositoryProvider repositoryProvider,
                                                 ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.fellesHåndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> køetBehandlingOpt = behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        forretningshendelseHåndtererFelles.fellesHåndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }
}
