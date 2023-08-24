package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.sakskompleks.BerørtBehandlingKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
public class BerørtBehandlingForvaltningTjeneste {

    private RevurderingTjeneste revurderingTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BerørtBehandlingKontroller berørtBehandlingTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    BerørtBehandlingForvaltningTjeneste() {
        //cdi
    }

    @Inject
    public BerørtBehandlingForvaltningTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste,
                                               BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                               BerørtBehandlingKontroller berørtBehandlingTjeneste,
                                               BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.revurderingTjeneste = revurderingTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.berørtBehandlingTjeneste = berørtBehandlingTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    void opprettNyBerørtBehandling(Fagsak fagsak) {
        if (!revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
            throw new ForvaltningException("Kan ikke opprette revurdering for fagsak " + fagsak.getId());
        }

        var behandlingÅrsakType = BehandlingÅrsakType.BERØRT_BEHANDLING;
        var nyBehandling = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, behandlingÅrsakType,
            behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
        berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(nyBehandling, behandlingÅrsakType);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(nyBehandling);
    }
}
