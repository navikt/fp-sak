package no.nav.foreldrepenger.mottak.hendelser.håndterer;

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
@ForretningshendelsestypeRef(ForretningshendelseType.DØDFØDSEL)
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
        if (!forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(åpenBehandling)) {
            forretningshendelseHåndtererFelles.håndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
        }
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        if (!forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(avsluttetBehandling)) {
            forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
        }
    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var køetBehandlingOpt = behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        if (køetBehandlingOpt.filter(forretningshendelseHåndtererFelles::barnFødselogDødAlleredeRegistrert).isPresent()) {
            return;
        }
        forretningshendelseHåndtererFelles.håndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }
}
