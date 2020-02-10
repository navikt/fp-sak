package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;


@ForretningshendelsestypeRef(ForretningshendelsestypeRef.YTELSE_HENDELSE)
@ApplicationScoped
public class YtelseForretningshendelseHåndterer implements ForretningshendelseHåndterer {

    private ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    public YtelseForretningshendelseHåndterer() {
        //Criminal Diamonds Inc.
    }

    @Inject
    public YtelseForretningshendelseHåndterer(BehandlingRepositoryProvider provider,
                                              ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles) {
        this.behandlingRevurderingRepository = provider.getBehandlingRevurderingRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
    }


    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        settBehandlingÅrsakPåBehandling(åpenBehandling, behandlingÅrsakType);
        forretningshendelseHåndtererFelles.fellesHåndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        // Her var det tidligere en blokkerende vent i IVEDSTEG. Nå lagres overlappende ytelse og iverksetting fullføres
        forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> køetBehandlingOpt = behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        forretningshendelseHåndtererFelles.fellesHåndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }

    private void settBehandlingÅrsakPåBehandling(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(behandling);
        behandlingRepository.lagre(behandling, lås);
    }
}
