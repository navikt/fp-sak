package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.UTFLYTTING)
public class UtflyttingForretningshendelseHåndterer implements ForretningshendelseHåndterer {

    private final ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private final BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @Inject
    public UtflyttingForretningshendelseHåndterer(BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                                  ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;

    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.håndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling,
                                           ForretningshendelseType forretningshendelseType,
                                           BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var køetBehandlingOpt = behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId());
        forretningshendelseHåndtererFelles.håndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }
}
