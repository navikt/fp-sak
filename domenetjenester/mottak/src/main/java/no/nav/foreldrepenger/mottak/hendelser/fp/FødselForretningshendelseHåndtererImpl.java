package no.nav.foreldrepenger.mottak.hendelser.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.FØDSEL)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class FødselForretningshendelseHåndtererImpl implements ForretningshendelseHåndterer {

    private final ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private final BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @Inject
    public FødselForretningshendelseHåndtererImpl(BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                                  ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;

    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (!forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(åpenBehandling)) {
            forretningshendelseHåndtererFelles.håndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
        }
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling,
                                           ForretningshendelseType forretningshendelseType,
                                           BehandlingÅrsakType behandlingÅrsakType) {
        if (!forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(avsluttetBehandling)) {
            forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
        }

    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var køetBehandlingOpt = behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId());
        if (køetBehandlingOpt.filter(forretningshendelseHåndtererFelles::barnFødselogDødAlleredeRegistrert).isPresent()) {
            return;
        }
        forretningshendelseHåndtererFelles.håndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }
}

